package rw.terimbere.csams.security;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;
import rw.terimbere.csams.modules.subscription.service.SubscriptionEntitlementService;

/**
 * Blocks cooperative operational writes when the selected cooperative's effective
 * subscription does not allow writes. Does not replace membership or permission checks.
 *
 * <p>SUPER_ADMIN bypasses. Auth, profile, onboarding, subscription reads, and Super Admin
 * cooperative administration stay exempt.
 */
@Component
@RequiredArgsConstructor
public class SubscriptionWriteInterceptor implements HandlerInterceptor {

    private static final Set<String> MUTATING_METHODS = Set.of("POST", "PUT", "PATCH", "DELETE");
    private static final Pattern COOPERATIVE_PATH = Pattern.compile(
            "^/api/v1/cooperatives/([0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12})(?:/.*)?$");

    private final SubscriptionEntitlementService entitlementService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod() == null ? "" : request.getMethod().toUpperCase();
        if (!MUTATING_METHODS.contains(method)) {
            return true;
        }

        String path = normalizedPath(request);
        if (isExempt(method, path)) {
            return true;
        }

        Matcher matcher = COOPERATIVE_PATH.matcher(path);
        if (!matcher.matches()) {
            return true;
        }

        UserPrincipal principal = currentPrincipal();
        if (principal == null) {
            return true;
        }
        if (principal.hasRole(CooperativeAuthorizationService.SUPER_ADMIN)) {
            return true;
        }

        UUID cooperativeId = UUID.fromString(matcher.group(1));
        if (!principal.isMemberOf(cooperativeId)) {
            return true;
        }

        entitlementService.requireWriteAllowed(cooperativeId);
        return true;
    }

    static boolean isExempt(String method, String path) {
        if (path.startsWith("/api/v1/auth/") || path.equals("/api/v1/auth")) {
            return true;
        }
        if (path.startsWith("/api/v1/onboarding/") || path.equals("/api/v1/onboarding")) {
            return true;
        }
        if (path.startsWith("/api/v1/public/")) {
            return true;
        }
        if (path.startsWith("/actuator/")) {
            return true;
        }
        if ("POST".equals(method) && path.equals("/api/v1/cooperatives")) {
            return true;
        }
        if ("PATCH".equals(method) && path.matches("^/api/v1/cooperatives/" + uuid() + "/status$")) {
            return true;
        }
        if ("POST".equals(method) && path.matches("^/api/v1/cooperatives/" + uuid() + "/administrators$")) {
            return true;
        }
        if (path.matches("^/api/v1/cooperatives/" + uuid() + "/subscription(?:/.*)?$")) {
            return true;
        }
        if (path.matches("^/api/v1/cooperatives/" + uuid() + "/billing(?:/.*)?$")) {
            return true;
        }
        if (path.matches("^/api/v1/cooperatives/" + uuid() + "/payments(?:/.*)?$")) {
            return true;
        }
        if ("POST".equals(method) && path.matches("^/api/v1/cooperatives/" + uuid() + "/reports/export$")) {
            return true;
        }
        if ("POST".equals(method)
                && path.matches("^/api/v1/cooperatives/" + uuid() + "/loans/repayment-preview$")) {
            return true;
        }
        return false;
    }

    private static String uuid() {
        return "[0-9a-fA-F]{8}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{4}-[0-9a-fA-F]{12}";
    }

    private static String normalizedPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String context = request.getContextPath();
        if (uri != null && context != null && !context.isEmpty() && uri.startsWith(context)) {
            uri = uri.substring(context.length());
        }
        if (uri == null || uri.isEmpty()) {
            return "/";
        }
        int query = uri.indexOf('?');
        return query >= 0 ? uri.substring(0, query) : uri;
    }

    private static UserPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal principal)) {
            return null;
        }
        return principal;
    }
}
