package com.example.scaffold.config;

import java.io.IOException;

import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.scaffold.common.ApiError;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import tools.jackson.databind.ObjectMapper;

/**
 * Enforces rate limits on every request. Endpoints annotated with
 * {@link RateLimit} use that method's settings (class-level annotations apply
 * to every method on the controller); everything else falls back to the
 * endpoint-type limits configured in {@link RateLimitConfig}, matching
 * "/api/auth/**", "/api/**" and default tiers.
 *
 * Runs after {@link com.example.scaffold.security.JwtAuthenticationFilter} so
 * the authenticated principal (if any) is already on the SecurityContext,
 * letting authenticated requests be limited per-user rather than per-IP.
 */
@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimitService rateLimitService;
    private final RateLimitConfig rateLimitConfig;
    private final RequestMappingHandlerMapping requestMappingHandlerMapping;
    private final ObjectMapper objectMapper;

    public RateLimitingFilter(RateLimitService rateLimitService, RateLimitConfig rateLimitConfig,
            RequestMappingHandlerMapping requestMappingHandlerMapping, ObjectMapper objectMapper) {
        this.rateLimitService = rateLimitService;
        this.rateLimitConfig = rateLimitConfig;
        this.requestMappingHandlerMapping = requestMappingHandlerMapping;
        this.objectMapper = objectMapper;
    }

    @Override
    protected boolean shouldNotFilter(HttpServletRequest request) {
        String path = request.getRequestURI();
        return !rateLimitConfig.isEnabled()
                || path.startsWith("/actuator/")
                || path.startsWith("/css/")
                || path.startsWith("/js/")
                || path.startsWith("/webjars/")
                || path.startsWith("/v3/api-docs")
                || path.startsWith("/swagger-ui");
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        RateLimit rateLimit = resolveAnnotation(request);
        String endpoint = resolveEndpoint(request);
        RateLimitConfig.RateLimitSettings settings = resolveSettings(rateLimit, endpoint);
        String key = resolveKey(rateLimit, request, endpoint);

        if (!rateLimitService.isAllowed(key, settings.getRequests(), settings.getWindow())) {
            long retryAfterSeconds = Math.max(rateLimitService.getTtl(key), 1);
            String message = rateLimit != null ? rateLimit.message() : "Rate limit exceeded. Please try again later.";
            log.debug("Rate limit exceeded for key: {}, retry after {}s", key, retryAfterSeconds);
            writeRateLimitExceeded(request, response, message, retryAfterSeconds);
            return;
        }

        int remaining = rateLimitService.getRemainingRequests(key, settings.getRequests());
        response.setHeader("X-RateLimit-Limit", String.valueOf(settings.getRequests()));
        response.setHeader("X-RateLimit-Remaining", String.valueOf(remaining));

        filterChain.doFilter(request, response);
    }

    private RateLimit resolveAnnotation(HttpServletRequest request) {
        try {
            HandlerExecutionChain chain = requestMappingHandlerMapping.getHandler(request);
            if (chain == null || !(chain.getHandler() instanceof HandlerMethod handlerMethod)) {
                return null;
            }
            RateLimit methodLevel = handlerMethod.getMethodAnnotation(RateLimit.class);
            if (methodLevel != null) {
                return methodLevel;
            }
            return handlerMethod.getBeanType().getAnnotation(RateLimit.class);
        } catch (Exception e) {
            log.debug("Could not resolve handler for rate limiting on {}: {}", request.getRequestURI(), e.getMessage());
            return null;
        }
    }

    private String resolveEndpoint(HttpServletRequest request) {
        // Must stay a bare path (no HTTP method prefix): RateLimitConfig.getSettingsForEndpoint
        // matches this directly against "/api/**"/"/auth/**" prefixes and configured
        // endpoint patterns such as "/api/auth/login".
        Object pattern = request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
        return pattern != null ? pattern.toString() : request.getRequestURI();
    }

    private RateLimitConfig.RateLimitSettings resolveSettings(RateLimit rateLimit, String endpoint) {
        if (rateLimit != null && rateLimit.requests() > 0 && rateLimit.window() > 0) {
            return new RateLimitConfig.RateLimitSettings(rateLimit.requests(), rateLimit.window());
        }
        return rateLimitConfig.getSettingsForEndpoint(endpoint);
    }

    private String resolveKey(RateLimit rateLimit, HttpServletRequest request, String endpoint) {
        String ip = clientIp(request);
        Long userId = currentUserId();
        // Endpoints without an explicit @RateLimit still get per-user limiting when
        // authenticated (satisfies requirement 4.3 by default); an explicit annotation's
        // own perUser value (default false) is honored as-is once someone opts in.
        boolean perUser = rateLimit == null || rateLimit.perUser();

        String pattern = rateLimit != null ? rateLimit.keyPattern() : "{ip}:{endpoint}";
        if (perUser && userId != null) {
            pattern = pattern.contains("{user}") ? pattern : "user:{user}:{endpoint}";
        }

        return pattern
                .replace("{ip}", ip)
                .replace("{user}", userId != null ? String.valueOf(userId) : "anonymous:" + ip)
                .replace("{endpoint}", endpoint);
    }

    private Long currentUserId() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.isAuthenticated()
                && authentication.getPrincipal() instanceof CustomUserPrincipal principal) {
            return principal.getId();
        }
        return null;
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp;
        }
        return request.getRemoteAddr();
    }

    private void writeRateLimitExceeded(HttpServletRequest request, HttpServletResponse response, String message,
            long retryAfterSeconds) throws IOException {
        response.setStatus(HttpStatus.TOO_MANY_REQUESTS.value());
        response.setHeader("Retry-After", String.valueOf(retryAfterSeconds));

        if (request.getRequestURI().startsWith(request.getContextPath() + "/api/")) {
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            ApiError error = new ApiError(HttpStatus.TOO_MANY_REQUESTS.value(),
                    HttpStatus.TOO_MANY_REQUESTS.getReasonPhrase(),
                    message + " Retry after " + retryAfterSeconds + " seconds.");
            objectMapper.writeValue(response.getWriter(), error);
        } else {
            response.setContentType(MediaType.TEXT_PLAIN_VALUE);
            response.getWriter().write(message + " Retry after " + retryAfterSeconds + " seconds.");
        }
    }
}
