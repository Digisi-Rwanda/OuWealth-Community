package rw.terimbere.csams.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.time.Instant;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import rw.terimbere.csams.shared.common.dto.ErrorResponse;

/**
 * Simple IP-based rate limiter for authentication endpoints (configurable requests / minute).
 */
@Component
public class AuthenticationRateLimitFilter extends OncePerRequestFilter {

    private static final long WINDOW_MS = 60_000L;

    private final ObjectMapper objectMapper;
    private final boolean enabled;
    private final int maxRequests;
    private final ConcurrentHashMap<String, Window> windows = new ConcurrentHashMap<>();

    public AuthenticationRateLimitFilter(
            ObjectMapper objectMapper,
            @Value("${app.security.auth-rate-limit-enabled:true}") boolean enabled,
            @Value("${app.security.auth-rate-limit-max:20}") int maxRequests) {
        this.objectMapper = objectMapper;
        this.enabled = enabled;
        this.maxRequests = maxRequests;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        if (!enabled) {
            return true;
        }
        String path = request.getRequestURI();
        return !(path.endsWith("/api/v1/auth/login")
                || path.endsWith("/api/v1/auth/signup")
                || path.endsWith("/api/v1/auth/bootstrap")
                || path.endsWith("/api/v1/onboarding/signup")
                || path.contains("/api/v1/auth/password-reset/"));
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String key = clientKey(request);
        long now = System.currentTimeMillis();
        Window window = windows.compute(key, (k, existing) -> {
            if (existing == null || now - existing.windowStart >= WINDOW_MS) {
                return new Window(now, new AtomicInteger(1));
            }
            existing.count.incrementAndGet();
            return existing;
        });

        if (window.count.get() > maxRequests) {
            response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ErrorResponse body = ErrorResponse.builder()
                    .timestamp(Instant.now())
                    .status(HttpStatus.TOO_MANY_REQUESTS.value())
                    .error(HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase())
                    .message("Too many authentication attempts. Please try again later.")
                    .path(request.getRequestURI())
                    .build();
            objectMapper.writeValue(response.getOutputStream(), body);
            return;
        }

        filterChain.doFilter(request, response);
    }

    private String clientKey(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            return forwarded.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private record Window(long windowStart, AtomicInteger count) {}
}
