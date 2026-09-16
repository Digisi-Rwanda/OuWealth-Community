package rw.terimbere.csams.modules.subscription.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

/**
 * Platform subscription prices for OuWealth. Kept in configuration (not a Plan table)
 * because MVP has a single global catalog, no per-cooperative price overrides, and
 * the project already centralizes similar knobs via {@code @ConfigurationProperties}.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "app.subscription")
public class SubscriptionProperties {

    private String currency = "RWF";
    private String timezone = "Africa/Kigali";
    private int trialMonths = 4;
    private int trialWarningDays = 14;
    private BigDecimal monthlyCharge = new BigDecimal("2000.0000");
    private BigDecimal annualListPrice = new BigDecimal("24000.0000");
    private BigDecimal annualCharge = new BigDecimal("18000.0000");
    private BigDecimal annualDiscountPercent = new BigDecimal("25");
    private Payment payment = new Payment();

    @Getter
    @Setter
    public static class Payment {
        private Mtn mtn = new Mtn();

        @Getter
        @Setter
        public static class Mtn {
            /**
             * Master switch. Local development stays up when this is false even if
             * credentials are blank. Production with enabled=true must supply secrets.
             */
            private boolean enabled = false;

            private String baseUrl = "https://sandbox.momodeveloper.mtn.com";
            private String subscriptionKey = "";
            private String apiUser = "";
            private String apiKey = "";
            /** Provider environment name (for example {@code sandbox} or {@code mtnrwanda}). Not inferred from the URL. */
            private String targetEnvironment = "sandbox";
            private String callbackUrl = "";
            private int connectTimeoutMs = 5_000;
            private int readTimeoutMs = 20_000;

            public boolean isConfigured() {
                return enabled
                        && StringUtils.hasText(baseUrl)
                        && StringUtils.hasText(subscriptionKey)
                        && StringUtils.hasText(apiUser)
                        && StringUtils.hasText(apiKey)
                        && StringUtils.hasText(targetEnvironment);
            }
        }
    }
}
