package rw.terimbere.csams.shared.exceptions;

/**
 * Phase 5 checkout contract: payment providers are not configured yet.
 * Does not record a payment or change subscription entitlement.
 */
public class PaymentIntegrationUnavailableException extends RuntimeException {

    public static final String CODE = "PAYMENT_INTEGRATION_UNAVAILABLE";

    public PaymentIntegrationUnavailableException() {
        super("Payment processing is not available yet. Your Saving Scheme was not charged.");
    }
}
