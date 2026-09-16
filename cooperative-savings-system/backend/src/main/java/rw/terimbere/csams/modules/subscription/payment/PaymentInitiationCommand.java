package rw.terimbere.csams.modules.subscription.payment;

import java.math.BigDecimal;
import java.util.UUID;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;

public record PaymentInitiationCommand(
        UUID paymentId,
        UUID cooperativeId,
        SubscriptionBillingCycle billingCycle,
        BigDecimal amount,
        String currency,
        String payerMsisdn) {}
