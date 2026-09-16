package rw.terimbere.csams.modules.subscription.controller;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import rw.terimbere.csams.modules.subscription.dto.BillingCheckoutRequest;
import rw.terimbere.csams.modules.subscription.dto.BillingPlansResponse;
import rw.terimbere.csams.modules.subscription.dto.SubscriptionPaymentResponse;
import rw.terimbere.csams.modules.subscription.service.BillingService;
import rw.terimbere.csams.shared.common.dto.ApiResponse;
import rw.terimbere.csams.shared.common.dto.PageResponse;
import rw.terimbere.csams.shared.pagination.PageMapper;

@RestController
@RequestMapping("/api/v1/cooperatives/{cooperativeId}/billing")
@RequiredArgsConstructor
@Tag(name = "Billing", description = "OuWealth platform subscription billing")
@SecurityRequirement(name = "bearerAuth")
public class BillingController {

    private final BillingService billingService;

    @GetMapping("/plans")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "OuWealth subscription plans and pricing for a cooperative")
    public ResponseEntity<ApiResponse<BillingPlansResponse>> plans(@PathVariable UUID cooperativeId) {
        return ResponseEntity.ok(ApiResponse.ok(billingService.getPlans(cooperativeId)));
    }

    @GetMapping("/payments")
    @PreAuthorize("isAuthenticated()")
    @Operation(summary = "Platform subscription payment history (not Saving Scheme ledger)")
    public ResponseEntity<ApiResponse<PageResponse<SubscriptionPaymentResponse>>> payments(
            @PathVariable UUID cooperativeId,
            @PageableDefault(size = 20, sort = "initiatedAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return ResponseEntity.ok(ApiResponse.ok(PageMapper.toPageResponse(billingService.listPayments(cooperativeId, pageable))));
    }

    @PostMapping("/checkout")
    @PreAuthorize("isAuthenticated()")
    @Operation(
            summary = "Start subscription checkout",
            description =
                    "Validates plan and payment method for authorized billing managers. "
                            + "Payment providers are not configured in this phase; the subscription is not charged.")
    public ResponseEntity<ApiResponse<Void>> checkout(
            @PathVariable UUID cooperativeId, @Valid @RequestBody BillingCheckoutRequest request) {
        billingService.checkout(cooperativeId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
