package rw.terimbere.csams.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.terimbere.csams.modules.subscription.payment.flutterwave.FlutterwaveWebhookPayload;
import rw.terimbere.csams.modules.subscription.service.BillingService;

/**
 * Unauthenticated Flutterwave webhook. Signature is checked, then BillingService re-queries
 * Flutterwave before changing entitlement. Unknown payments return 200 after a valid hash.
 */
@RestController
@RequestMapping("/api/v1/public/billing/flutterwave")
@RequiredArgsConstructor
@Tag(name = "Billing callbacks", description = "Flutterwave hosted-checkout webhooks")
public class FlutterwaveWebhookController {

    private final BillingService billingService;

    @PostMapping("/webhook")
    @Operation(summary = "Flutterwave charge webhook")
    @Hidden
    public ResponseEntity<Void> webhook(
            @RequestHeader(value = "verif-hash", required = false) String verifHash,
            @RequestBody(required = false) FlutterwaveWebhookPayload body) {
        billingService.handleFlutterwaveWebhook(verifHash, body);
        return ResponseEntity.ok().build();
    }
}
