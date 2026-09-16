package rw.terimbere.csams.modules.subscription.config;

import java.math.BigDecimal;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

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
}
