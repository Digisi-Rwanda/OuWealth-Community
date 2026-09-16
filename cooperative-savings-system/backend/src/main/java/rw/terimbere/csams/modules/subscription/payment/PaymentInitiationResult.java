package rw.terimbere.csams.modules.subscription.payment;

public record PaymentInitiationResult(boolean accepted, String externalReference, String failureMessage) {

    public static PaymentInitiationResult accepted(String externalReference) {
        return new PaymentInitiationResult(true, externalReference, null);
    }

    public static PaymentInitiationResult rejected(String failureMessage) {
        return new PaymentInitiationResult(false, null, failureMessage);
    }
}
