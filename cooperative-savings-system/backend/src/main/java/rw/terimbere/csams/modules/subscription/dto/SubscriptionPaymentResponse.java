package rw.terimbere.csams.modules.subscription.dto;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentStatus;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubscriptionPaymentResponse {

    private UUID id;
    private SubscriptionBillingCycle billingCycle;
    private SubscriptionPaymentChannel paymentChannel;
    private SubscriptionPaymentStatus status;
    private String currency;
    private BigDecimal amount;
    private String provider;
    private String externalReference;
    private Instant initiatedAt;
    private Instant paidAt;
    private Instant failedAt;
}
