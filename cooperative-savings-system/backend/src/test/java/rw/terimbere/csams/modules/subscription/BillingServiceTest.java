package rw.terimbere.csams.modules.subscription;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import rw.terimbere.csams.modules.subscription.config.SubscriptionProperties;
import rw.terimbere.csams.modules.subscription.dto.BillingCheckoutRequest;
import rw.terimbere.csams.modules.subscription.dto.BillingPlanQuote;
import rw.terimbere.csams.modules.subscription.dto.BillingPlansResponse;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionBillingCycle;
import rw.terimbere.csams.modules.subscription.entity.SubscriptionPaymentChannel;
import rw.terimbere.csams.modules.subscription.repository.SubscriptionPaymentRepository;
import rw.terimbere.csams.modules.subscription.service.BillingService;
import rw.terimbere.csams.modules.subscription.service.SubscriptionPricing;
import rw.terimbere.csams.security.CooperativeAuthorizationService;
import rw.terimbere.csams.security.UserPrincipal;
import rw.terimbere.csams.shared.exceptions.ForbiddenException;
import rw.terimbere.csams.shared.exceptions.PaymentIntegrationUnavailableException;

@ExtendWith(MockitoExtension.class)
class BillingServiceTest {

    @Mock
    private CooperativeAuthorizationService authorizationService;

    @Mock
    private SubscriptionPaymentRepository paymentRepository;

    private BillingService billingService;
    private SubscriptionPricing pricing;

    @BeforeEach
    void setUp() {
        pricing = new SubscriptionPricing(new SubscriptionProperties());
        pricing.validateCatalog();
        billingService = new BillingService(authorizationService, pricing, paymentRepository);
    }

    @Test
    void catalogIsSourcedFromSubscriptionPricing() {
        BillingPlansResponse catalog = billingService.catalog();
        assertThat(catalog.getCurrency()).isEqualTo(pricing.currency());
        assertThat(catalog.getTrialMonths()).isEqualTo(pricing.trialMonths());

        BillingPlanQuote monthly = catalog.getPlans().get(0);
        assertThat(monthly.getBillingCycle()).isEqualTo(SubscriptionBillingCycle.MONTHLY);
        assertThat(monthly.getAmount()).isEqualByComparingTo(pricing.monthly().charge());
        assertThat(monthly.getListPrice()).isEqualByComparingTo(pricing.monthly().listPrice());
        assertThat(monthly.getDiscountPercent()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(monthly.getSavings()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(monthly.getPeriodMonths()).isEqualTo(1);

        BillingPlanQuote annual = catalog.getPlans().get(1);
        assertThat(annual.getBillingCycle()).isEqualTo(SubscriptionBillingCycle.ANNUAL);
        assertThat(annual.getListPrice()).isEqualByComparingTo(pricing.annual().listPrice());
        assertThat(annual.getAmount()).isEqualByComparingTo(pricing.annual().charge());
        assertThat(annual.getDiscountPercent()).isEqualByComparingTo(pricing.annual().discountPercent());
        assertThat(annual.getSavings()).isEqualByComparingTo("6000.0000");
        assertThat(annual.getPeriodMonths()).isEqualTo(12);
    }

    @Test
    void getPlansRequiresMembership() {
        UUID cooperativeId = UUID.randomUUID();
        billingService.getPlans(cooperativeId);
        verify(authorizationService).requireMembership(cooperativeId);
    }

    @Test
    void checkoutRequiresBillingManager() {
        UUID cooperativeId = UUID.randomUUID();
        when(authorizationService.currentPrincipal()).thenReturn(principal("MEMBER"));
        assertThatThrownBy(() -> billingService.checkout(cooperativeId, checkoutRequest()))
                .isInstanceOf(ForbiddenException.class);
        verify(paymentRepository, never()).save(any());
    }

    @Test
    void checkoutDoesNotActivateUntilPaymentIntegrationExists() {
        UUID cooperativeId = UUID.randomUUID();
        when(authorizationService.currentPrincipal()).thenReturn(principal("PRESIDENT"));
        assertThatThrownBy(() -> billingService.checkout(cooperativeId, checkoutRequest()))
                .isInstanceOf(PaymentIntegrationUnavailableException.class)
                .hasMessageContaining("was not charged");
        verify(paymentRepository, never()).save(any());
    }

    private static BillingCheckoutRequest checkoutRequest() {
        return BillingCheckoutRequest.builder()
                .billingCycle(SubscriptionBillingCycle.MONTHLY)
                .paymentChannel(SubscriptionPaymentChannel.MTN_MOMO)
                .build();
    }

    private static UserPrincipal principal(String role) {
        return UserPrincipal.builder()
                .username("tester")
                .roles(Set.of(role))
                .permissions(Set.of())
                .cooperativeIds(Set.of())
                .accountNonLocked(true)
                .enabled(true)
                .build();
    }
}
