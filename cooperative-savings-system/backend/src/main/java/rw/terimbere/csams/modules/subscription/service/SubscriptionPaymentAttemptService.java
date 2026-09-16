package rw.terimbere.csams.modules.subscription.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.time.Clock;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.audit.service.AuditService;
import rw.terimbere.csams.modules.subscription.entity.CooperativeSubscription;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPayment;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentStatus;
import rw.terimbere.csams.modules.subscription.payment.mtn.MtnMomoSubscriptionPaymentProvider;
import rw.terimbere.csams.modules.subscription.repository.CooperativeSubscriptionRepository;
import rw.terimbere.csams.modules.subscription.repository.SubscriptionPaymentRepository;
import rw.terimbere.csams.shared.auditing.AuditableAction;
import rw.terimbere.csams.shared.exceptions.ResourceNotFoundException;

/**
 * Transactional writes for checkout attempts. HTTP calls to MTN stay outside this bean.
 */
@Service
@RequiredArgsConstructor
public class SubscriptionPaymentAttemptService {

    static final String PENDING_MESSAGE = "Payment request sent. Approve the payment on your phone.";
    static final String FAILED_MESSAGE = "Payment was not completed.";

    private final CooperativeSubscriptionRepository subscriptionRepository;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionPricing pricing;
    private final AuditService auditService;
    private final ObjectMapper objectMapper;
    private final Clock clock;

    @Transactional
    public SubscriptionPayment createOrReusePending(
            UUID cooperativeId,
            SubscriptionBillingCycle billingCycle,
            SubscriptionPaymentChannel paymentChannel,
            UUID actorUserId) {
        CooperativeSubscription subscription = subscriptionRepository
                .findByCooperativeIdForUpdate(cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException("CooperativeSubscription", cooperativeId));

        SubscriptionPayment existing = paymentRepository
                .findFirstByCooperativeIdAndBillingCycleAndPaymentChannelAndStatusOrderByInitiatedAtDesc(
                        cooperativeId, billingCycle, paymentChannel, SubscriptionPaymentStatus.PENDING)
                .orElse(null);
        if (existing != null) {
            return existing;
        }

        Instant now = clock.instant();
        var quote = pricing.quote(billingCycle);
        SubscriptionPayment payment = SubscriptionPayment.builder()
                .subscriptionId(subscription.getId())
                .cooperativeId(cooperativeId)
                .billingCycle(billingCycle)
                .paymentChannel(paymentChannel)
                .status(SubscriptionPaymentStatus.PENDING)
                .currency(pricing.currency())
                .amount(quote.charge())
                .provider(MtnMomoSubscriptionPaymentProvider.PROVIDER_NAME)
                .initiatedAt(now)
                .build();
        SubscriptionPayment saved = paymentRepository.saveAndFlush(payment);
        saved.setIdempotencyKey("mtn-momo:" + saved.getId());
        saved = paymentRepository.saveAndFlush(saved);

        auditService.record(
                actorUserId,
                cooperativeId,
                AuditableAction.SUBSCRIPTION_PAYMENT_INITIATED,
                "SubscriptionPayment",
                saved.getId(),
                null,
                json(initiatedSnapshot(saved)),
                null,
                null);
        return saved;
    }

    @Transactional
    public SubscriptionPayment markInitiated(UUID paymentId, String externalReference) {
        SubscriptionPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPayment", paymentId));
        if (payment.getStatus() != SubscriptionPaymentStatus.PENDING) {
            return payment;
        }
        if (StringUtils.hasText(externalReference)) {
            payment.setExternalReference(externalReference);
        } else if (!StringUtils.hasText(payment.getExternalReference())) {
            payment.setExternalReference(payment.getId().toString());
        }
        payment.setProvider(MtnMomoSubscriptionPaymentProvider.PROVIDER_NAME);
        return paymentRepository.save(payment);
    }

    @Transactional
    public SubscriptionPayment markFailed(UUID paymentId, UUID actorUserId) {
        return markTerminal(paymentId, SubscriptionPaymentStatus.FAILED, actorUserId);
    }

    @Transactional
    public SubscriptionPayment markCanceled(UUID paymentId, UUID actorUserId) {
        return markTerminal(paymentId, SubscriptionPaymentStatus.CANCELED, actorUserId);
    }

    @Transactional
    public SubscriptionPayment markTerminal(
            UUID paymentId, SubscriptionPaymentStatus terminal, UUID actorUserId) {
        SubscriptionPayment payment = paymentRepository
                .findByIdForUpdate(paymentId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPayment", paymentId));
        if (payment.getStatus() == SubscriptionPaymentStatus.SUCCESS
                || payment.getStatus() == SubscriptionPaymentStatus.CANCELED
                || payment.getStatus() == SubscriptionPaymentStatus.FAILED) {
            return payment;
        }
        Instant now = clock.instant();
        payment.setStatus(terminal);
        payment.setFailedAt(now);
        SubscriptionPayment saved = paymentRepository.save(payment);
        auditService.record(
                actorUserId,
                saved.getCooperativeId(),
                AuditableAction.SUBSCRIPTION_PAYMENT_FAILED,
                "SubscriptionPayment",
                saved.getId(),
                null,
                json(initiatedSnapshot(saved)),
                null,
                null);
        return saved;
    }

    private Map<String, Object> initiatedSnapshot(SubscriptionPayment payment) {
        Map<String, Object> values = new LinkedHashMap<>();
        values.put("paymentId", payment.getId());
        values.put("cooperativeId", payment.getCooperativeId());
        values.put("billingCycle", payment.getBillingCycle() == null ? null : payment.getBillingCycle().name());
        values.put(
                "paymentChannel",
                payment.getPaymentChannel() == null ? null : payment.getPaymentChannel().name());
        values.put("amount", payment.getAmount());
        values.put("currency", payment.getCurrency());
        values.put("status", payment.getStatus() == null ? null : payment.getStatus().name());
        values.put("provider", payment.getProvider());
        values.put("externalReference", payment.getExternalReference());
        return values;
    }

    private String json(Map<String, Object> values) {
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Unable to serialize subscription payment audit payload", e);
        }
    }
}
