package rw.terimbere.csams.modules.subscription.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.report.whatsapp.WhatsAppPhone;
import rw.terimbere.csams.modules.subscription.dto.BillingCheckoutRequest;
import rw.terimbere.csams.modules.subscription.dto.BillingCheckoutResponse;
import rw.terimbere.csams.modules.subscription.dto.BillingPlanQuote;
import rw.terimbere.csams.modules.subscription.dto.BillingPlansResponse;
import rw.terimbere.csams.modules.subscription.dto.SubscriptionPaymentResponse;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPayment;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentStatus;
import rw.terimbere.csams.modules.subscription.payment.PaymentInitiationCommand;
import rw.terimbere.csams.modules.subscription.payment.PaymentInitiationResult;
import rw.terimbere.csams.modules.subscription.payment.ProviderPaymentStatus;
import rw.terimbere.csams.modules.subscription.payment.SubscriptionPaymentProvider;
import rw.terimbere.csams.modules.subscription.payment.SubscriptionPaymentProviderRegistry;
import rw.terimbere.csams.modules.subscription.payment.mtn.MtnMomoSubscriptionPaymentProvider;
import rw.terimbere.csams.modules.subscription.repository.SubscriptionPaymentRepository;
import rw.terimbere.csams.security.CooperativeAuthorizationService;
import rw.terimbere.csams.security.CooperativeOfficerRoles;
import rw.terimbere.csams.security.UserPrincipal;
import rw.terimbere.csams.shared.exceptions.PaymentIntegrationUnavailableException;
import rw.terimbere.csams.shared.exceptions.ResourceNotFoundException;
import rw.terimbere.csams.shared.exceptions.ValidationException;
import rw.terimbere.csams.shared.utilities.MoneyUtils;
import rw.terimbere.csams.shared.validation.CooperativeFieldRules;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final CooperativeAuthorizationService authorizationService;
    private final SubscriptionPricing pricing;
    private final SubscriptionPaymentRepository paymentRepository;
    private final SubscriptionPaymentProviderRegistry providerRegistry;
    private final SubscriptionPaymentAttemptService attemptService;
    private final SubscriptionActivationService activationService;

    @Transactional(readOnly = true)
    public BillingPlansResponse getPlans(UUID cooperativeId) {
        authorizationService.requireMembership(cooperativeId);
        return catalog();
    }

    public BillingPlansResponse catalog() {
        return BillingPlansResponse.builder()
                .currency(pricing.currency())
                .trialMonths(pricing.trialMonths())
                .plans(List.of(toQuote(pricing.monthly()), toQuote(pricing.annual())))
                .build();
    }

    @Transactional(readOnly = true)
    public Page<SubscriptionPaymentResponse> listPayments(UUID cooperativeId, Pageable pageable) {
        authorizationService.requireMembership(cooperativeId);
        return paymentRepository.findByCooperativeId(cooperativeId, pageable).map(this::toPayment);
    }

    /**
     * Starts MTN Collection checkout. Does not activate the subscription.
     * Amount is always taken from {@link SubscriptionPricing}; client amount fields are ignored.
     */
    public BillingCheckoutResponse checkout(UUID cooperativeId, BillingCheckoutRequest request) {
        authorizationService.requireMembership(cooperativeId);
        UserPrincipal principal = authorizationService.currentPrincipal();
        CooperativeOfficerRoles.requireBillingManager(principal);
        if (request == null || request.getBillingCycle() == null || request.getPaymentChannel() == null) {
            throw new ValidationException("Billing cycle and payment method are required");
        }
        pricing.quote(request.getBillingCycle());

        if (request.getPaymentChannel() == SubscriptionPaymentChannel.CARD) {
            throw new PaymentIntegrationUnavailableException();
        }

        SubscriptionPaymentProvider provider = providerRegistry.requireAvailable(request.getPaymentChannel());
        String msisdn = requireMtnMsisdn(request);
        SubscriptionPayment payment = attemptService.createOrReusePending(
                cooperativeId, request.getBillingCycle(), request.getPaymentChannel(), actorId(principal));

        if (payment.getStatus() == SubscriptionPaymentStatus.PENDING
                && StringUtils.hasText(payment.getExternalReference())) {
            return toCheckout(payment, SubscriptionPaymentAttemptService.PENDING_MESSAGE);
        }

        PaymentInitiationResult result = provider.initiate(new PaymentInitiationCommand(
                payment.getId(),
                cooperativeId,
                payment.getBillingCycle(),
                payment.getAmount(),
                payment.getCurrency(),
                msisdn));
        if (!result.accepted()) {
            payment = attemptService.markFailed(payment.getId(), actorId(principal));
            return toCheckout(payment, SubscriptionPaymentAttemptService.FAILED_MESSAGE);
        }
        payment = attemptService.markInitiated(payment.getId(), result.externalReference());
        return toCheckout(payment, SubscriptionPaymentAttemptService.PENDING_MESSAGE);
    }

    public SubscriptionPaymentResponse getPayment(UUID cooperativeId, UUID paymentId) {
        authorizationService.requireMembership(cooperativeId);
        SubscriptionPayment payment = paymentRepository
                .findByIdAndCooperativeId(paymentId, cooperativeId)
                .orElseThrow(() -> new ResourceNotFoundException("SubscriptionPayment", paymentId));
        return toPayment(synchronizeWithProvider(payment, actorId(authorizationService.currentPrincipal())));
    }

    /**
     * MTN Collection callback. Payload status is ignored; provider status is re-queried.
     */
    public void handleMtnCallback(String referenceId) {
        if (!StringUtils.hasText(referenceId)) {
            return;
        }
        SubscriptionPayment payment = paymentRepository
                .findByProviderAndExternalReference(
                        MtnMomoSubscriptionPaymentProvider.PROVIDER_NAME, referenceId.trim())
                .or(() -> parseUuid(referenceId).flatMap(paymentRepository::findById))
                .orElse(null);
        if (payment == null) {
            return;
        }
        synchronizeWithProvider(payment, null);
    }

    SubscriptionPayment synchronizeWithProvider(SubscriptionPayment payment, UUID actorUserId) {
        if (payment.getStatus() == SubscriptionPaymentStatus.SUCCESS
                || payment.getStatus() == SubscriptionPaymentStatus.CANCELED) {
            return payment;
        }
        if (payment.getPaymentChannel() != SubscriptionPaymentChannel.MTN_MOMO) {
            return payment;
        }
        SubscriptionPaymentProvider provider = providerRegistry.find(SubscriptionPaymentChannel.MTN_MOMO);
        if (provider == null || !provider.available()) {
            return payment;
        }
        String reference = StringUtils.hasText(payment.getExternalReference())
                ? payment.getExternalReference()
                : payment.getId().toString();
        ProviderPaymentStatus providerStatus = provider.verify(reference);
        return switch (providerStatus) {
            case SUCCESS -> {
                activationService.applySuccessfulPayment(payment.getId(), actorUserId);
                yield paymentRepository.findById(payment.getId()).orElse(payment);
            }
            case FAILED -> attemptService.markFailed(payment.getId(), actorUserId);
            case CANCELED -> attemptService.markCanceled(payment.getId(), actorUserId);
            case PENDING, UNKNOWN -> payment;
        };
    }

    private static String requireMtnMsisdn(BillingCheckoutRequest request) {
        if (request.getPaymentChannel() != SubscriptionPaymentChannel.MTN_MOMO) {
            return null;
        }
        if (!StringUtils.hasText(request.getPayerPhoneNumber())
                || !CooperativeFieldRules.isValidRwandanPhone(request.getPayerPhoneNumber())) {
            throw new ValidationException("A valid Rwandan MTN Mobile Money phone number is required");
        }
        String msisdn = WhatsAppPhone.toRecipient(request.getPayerPhoneNumber());
        if (!StringUtils.hasText(msisdn)) {
            throw new ValidationException("A valid Rwandan MTN Mobile Money phone number is required");
        }
        return msisdn;
    }

    private static UUID actorId(UserPrincipal principal) {
        return principal == null ? null : principal.getId();
    }

    private static java.util.Optional<UUID> parseUuid(String value) {
        try {
            return java.util.Optional.of(UUID.fromString(value.trim()));
        } catch (RuntimeException ex) {
            return java.util.Optional.empty();
        }
    }

    private BillingPlanQuote toQuote(SubscriptionPricing.PlanQuote quote) {
        return BillingPlanQuote.builder()
                .billingCycle(quote.billingCycle())
                .listPrice(quote.listPrice())
                .amount(quote.charge())
                .discountPercent(quote.discountPercent())
                .savings(MoneyUtils.subtract(quote.listPrice(), quote.charge()))
                .periodMonths(quote.periodMonths())
                .build();
    }

    private BillingCheckoutResponse toCheckout(SubscriptionPayment payment, String message) {
        return BillingCheckoutResponse.builder()
                .paymentId(payment.getId())
                .status(payment.getStatus())
                .billingCycle(payment.getBillingCycle())
                .paymentChannel(payment.getPaymentChannel())
                .amount(payment.getAmount())
                .currency(payment.getCurrency())
                .message(message)
                .build();
    }

    private SubscriptionPaymentResponse toPayment(SubscriptionPayment payment) {
        return SubscriptionPaymentResponse.builder()
                .id(payment.getId())
                .billingCycle(payment.getBillingCycle())
                .paymentChannel(payment.getPaymentChannel())
                .status(payment.getStatus())
                .currency(payment.getCurrency())
                .amount(payment.getAmount())
                .provider(payment.getProvider())
                .externalReference(payment.getExternalReference())
                .initiatedAt(payment.getInitiatedAt())
                .paidAt(payment.getPaidAt())
                .failedAt(payment.getFailedAt())
                .build();
    }
}
