package rw.terimbere.csams.configuration;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.core.env.Environment;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import rw.terimbere.csams.modules.subscription.config.SubscriptionProperties;

/**
 * Fail-fast production guard: refuse to start with blank or local-default JWT/DB secrets.
 * Active only for the {@code production} profile — local/test/staging are unaffected.
 */
@Component
@Profile("production")
@RequiredArgsConstructor
public class ProductionSecretsValidator implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(ProductionSecretsValidator.class);

    private final JwtProperties jwtProperties;
    private final SubscriptionProperties subscriptionProperties;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) {
        List<String> failures = new ArrayList<>();

        validateJwtSecret("app.jwt.access-secret / JWT_ACCESS_SECRET", jwtProperties.getAccessSecret(), failures);
        validateJwtSecret("app.jwt.refresh-secret / JWT_REFRESH_SECRET", jwtProperties.getRefreshSecret(), failures);

        requireEnv("POSTGRES_USER", failures);
        requireEnv("POSTGRES_PASSWORD", failures);
        requireEnv("POSTGRES_DB", failures);

        if (subscriptionProperties.getPayment().getMtn().isEnabled()) {
            requireEnv("MTN_MOMO_SUBSCRIPTION_KEY", failures);
            requireEnv("MTN_MOMO_API_USER", failures);
            requireEnv("MTN_MOMO_API_KEY", failures);
            requireEnv("MTN_MOMO_BASE_URL", failures);
            requireEnv("MTN_MOMO_TARGET_ENVIRONMENT", failures);
        }

        if (subscriptionProperties.getPayment().getFlutterwave().isEnabled()) {
            requireEnv("FLUTTERWAVE_BASE_URL", failures);
            requireEnv("FLUTTERWAVE_SECRET_KEY", failures);
            requireEnv("FLUTTERWAVE_SECRET_HASH", failures);
            requireEnv("FLUTTERWAVE_REDIRECT_URL", failures);
        }

        if (!failures.isEmpty()) {
            String message = "Production secrets validation failed:\n - " + String.join("\n - ", failures);
            log.error(message);
            throw new IllegalStateException(message);
        }

        log.info("Production secrets validation passed");
    }

    private void validateJwtSecret(String name, String value, List<String> failures) {
        if (!StringUtils.hasText(value)) {
            failures.add(name + " is missing or blank");
            return;
        }
        String lower = value.toLowerCase(Locale.ROOT);
        if (lower.contains("change-me") || lower.contains("local-dev")) {
            failures.add(name + " must not use a local/default value containing 'change-me' or 'local-dev'");
        }
    }

    private void requireEnv(String name, List<String> failures) {
        String value = environment.getProperty(name);
        if (!StringUtils.hasText(value)) {
            failures.add(name + " environment variable is missing or blank");
        }
    }
}
