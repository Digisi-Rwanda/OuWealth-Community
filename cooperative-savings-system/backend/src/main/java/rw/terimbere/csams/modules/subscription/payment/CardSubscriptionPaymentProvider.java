package rw.terimbere.csams.modules.subscription.payment;

import org.springframework.stereotype.Component;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.shared.exceptions.PaymentIntegrationUnavailableException;

/**
 * Phase 6A contract for hosted card checkout. No card numbers are collected here,
 * and success is never faked.
 */
@Component
public class CardSubscriptionPaymentProvider implements SubscriptionPaymentProvider {

    @Override
    public SubscriptionPaymentChannel channel() {
        return SubscriptionPaymentChannel.CARD;
    }

    @Override
    public boolean available() {
        return false;
    }

    @Override
    public PaymentInitiationResult initiate(PaymentInitiationCommand command) {
        throw new PaymentIntegrationUnavailableException();
    }

    @Override
    public ProviderPaymentStatus verify(String externalReference) {
        throw new PaymentIntegrationUnavailableException();
    }
}
