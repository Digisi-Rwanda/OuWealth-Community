package rw.terimbere.csams.modules.subscription.service;

import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import rw.terimbere.csams.modules.subscription.dto.BillingCheckoutRequest;
import rw.terimbere.csams.modules.subscription.dto.BillingPlanQuote;
import rw.terimbere.csams.modules.subscription.dto.BillingPlansResponse;
import rw.terimbere.csams.modules.subscription.dto.SubscriptionPaymentResponse;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPayment;
import rw.terimbere.csams.modules.subscription.repository.SubscriptionPaymentRepository;
import rw.terimbere.csams.security.CooperativeAuthorizationService;
import rw.terimbere.csams.security.CooperativeOfficerRoles;
import rw.terimbere.csams.shared.exceptions.PaymentIntegrationUnavailableException;
import rw.terimbere.csams.shared.exceptions.ValidationException;
import rw.terimbere.csams.shared.utilities.MoneyUtils;

@Service
@RequiredArgsConstructor
public class BillingService {

    private final CooperativeAuthorizationService authorizationService;
    private final SubscriptionPricing pricing;
    private final SubscriptionPaymentRepository paymentRepository;

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
     * Phase 6 will invoke the payment provider. Phase 5 validates authorization and
     * the checkout payload, then refuses to charge or activate a subscription.
     */
    public void checkout(UUID cooperativeId, BillingCheckoutRequest request) {
        authorizationService.requireMembership(cooperativeId);
        CooperativeOfficerRoles.requireBillingManager(authorizationService.currentPrincipal());
        if (request == null || request.getBillingCycle() == null || request.getPaymentChannel() == null) {
            throw new ValidationException("Billing cycle and payment method are required");
        }
        pricing.quote(request.getBillingCycle());
        throw new PaymentIntegrationUnavailableException();
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
