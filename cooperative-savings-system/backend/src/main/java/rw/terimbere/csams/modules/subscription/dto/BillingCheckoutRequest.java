package rw.terimbere.csams.modules.subscription.dto;

import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BillingCheckoutRequest {

    @NotNull
    private SubscriptionBillingCycle billingCycle;

    @NotNull
    private SubscriptionPaymentChannel paymentChannel;
}
