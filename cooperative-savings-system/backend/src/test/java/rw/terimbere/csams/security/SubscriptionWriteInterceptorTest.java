package rw.terimbere.csams.security;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubscriptionWriteInterceptorTest {

    @Test
    void exemptsAuthOnboardingPublicAndSuperAdminAdminPaths() {
        assertThat(SubscriptionWriteInterceptor.isExempt("POST", "/api/v1/auth/login")).isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt("PATCH", "/api/v1/auth/me")).isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt("POST", "/api/v1/onboarding/signup")).isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt("POST", "/api/v1/cooperatives")).isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "PATCH", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/status"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/administrators"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "GET", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/subscription"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "GET", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/billing/plans"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "GET", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/billing/payments"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/billing/checkout"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "GET",
                        "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/billing/payments"
                                + "/22222222-2222-2222-2222-222222222222"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt("POST", "/api/v1/public/billing/flutterwave/webhook"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST",
                        "/api/v1/public/billing/mtn/callback/22222222-2222-2222-2222-222222222222"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/payments"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/reports/export"))
                .isTrue();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST",
                        "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/loans/repayment-preview"))
                .isTrue();
    }

    @Test
    void doesNotExemptOperationalWrites() {
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/members"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "PUT", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/contributions/period"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/loans"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "PUT", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/logo"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST",
                        "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/reports/share-whatsapp"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST",
                        "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/loans"
                                + "/22222222-2222-2222-2222-222222222222/schedule/share-whatsapp"))
                .isFalse();
        assertThat(SubscriptionWriteInterceptor.isExempt(
                        "POST", "/api/v1/cooperatives/11111111-1111-1111-1111-111111111111/reports"))
                .isFalse();
    }
}
