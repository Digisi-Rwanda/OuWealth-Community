package rw.terimbere.csams.modules.cooperative;

import java.util.UUID;

/** Shared create-body helpers for cooperative integration tests. */
public final class CooperativeTestFixtures {

    private CooperativeTestFixtures() {}

    public static String createBody(String name) {
        return createBody(name, "1000.0000", 1);
    }

    public static String createBody(String name, String monthlyAmount, int dueDay) {
        String suffix = UUID.randomUUID().toString().substring(0, 8).toUpperCase();
        return """
                {
                  "name":"%s",
                  "currency":"RWF",
                  "monthlyContributionAmount":%s,
                  "contributionDueDay":%d,
                  "financialYearStartMonth":1,
                  "registrationNumber":"RCA/TEST/%s",
                  "contactEmail":"coop-%s@test.local",
                  "contactPhone":"0781234567",
                  "registrationDate":"2024-01-15"
                }
                """
                .formatted(name, monthlyAmount, dueDay, suffix, suffix.toLowerCase());
    }

    /** Appends extra JSON object fields (without a leading comma) before the closing brace. */
    public static String createBodyWith(String name, String extraFieldsJson) {
        String body = createBody(name);
        if (extraFieldsJson == null || extraFieldsJson.isBlank()) {
            return body;
        }
        String extra = extraFieldsJson.trim();
        if (!extra.startsWith(",")) {
            extra = "," + extra;
        }
        return body.substring(0, body.lastIndexOf('}')) + extra + "}";
    }
}
