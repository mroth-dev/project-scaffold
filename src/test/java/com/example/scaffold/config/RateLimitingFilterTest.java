package com.example.scaffold.config;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockFilterChain;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;
import org.springframework.web.servlet.HandlerExecutionChain;
import org.springframework.web.servlet.HandlerMapping;
import org.springframework.web.servlet.mvc.method.annotation.RequestMappingHandlerMapping;

import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

import tools.jackson.databind.ObjectMapper;

class RateLimitingFilterTest {

    private RateLimitService rateLimitService;
    private RateLimitConfig rateLimitConfig;
    private RequestMappingHandlerMapping handlerMapping;
    private RateLimitingFilter filter;

    @BeforeEach
    void setUp() throws Exception {
        rateLimitService = mock(RateLimitService.class);
        rateLimitConfig = mock(RateLimitConfig.class);
        handlerMapping = mock(RequestMappingHandlerMapping.class);
        when(rateLimitConfig.isEnabled()).thenReturn(true);
        when(rateLimitConfig.getSettingsForEndpoint(anyString()))
                .thenReturn(new RateLimitConfig.RateLimitSettings(100, 60));
        when(handlerMapping.getHandler(any())).thenReturn(null);
        filter = new RateLimitingFilter(rateLimitService, rateLimitConfig, handlerMapping, new ObjectMapper());
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void skipsActuatorAndStaticAssetPaths() {
        assertTrue(filter.shouldNotFilter(requestFor("/actuator/health")));
        assertTrue(filter.shouldNotFilter(requestFor("/css/app.css")));
        assertTrue(filter.shouldNotFilter(requestFor("/js/app.js")));
        assertTrue(filter.shouldNotFilter(requestFor("/webjars/htmx.org/htmx.min.js")));
        assertTrue(filter.shouldNotFilter(requestFor("/v3/api-docs")));
        assertTrue(filter.shouldNotFilter(requestFor("/swagger-ui/index.html")));
        assertFalse(filter.shouldNotFilter(requestFor("/api/products")));
    }

    @Test
    void shouldNotFilterWhenDisabledGlobally() {
        when(rateLimitConfig.isEnabled()).thenReturn(false);

        assertTrue(filter.shouldNotFilter(requestFor("/api/products")));
    }

    @Test
    void allowsRequestAndSetsRateLimitHeaders() throws Exception {
        MockHttpServletRequest request = requestFor("/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(rateLimitService.isAllowed(anyString(), eq(100), eq(60))).thenReturn(true);
        when(rateLimitService.getRemainingRequests(anyString(), eq(100))).thenReturn(97);

        filter.doFilter(request, response, chain);

        assertEquals(request, chain.getRequest());
        assertEquals("100", response.getHeader("X-RateLimit-Limit"));
        assertEquals("97", response.getHeader("X-RateLimit-Remaining"));
    }

    @Test
    void blocksApiRequestWithJsonBodyWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = requestFor("/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(rateLimitService.getTtl(anyString())).thenReturn(30L);

        filter.doFilter(request, response, chain);

        assertNull(chain.getRequest(), "downstream chain must not run once the limit is exceeded");
        assertEquals(429, response.getStatus());
        assertEquals("30", response.getHeader("Retry-After"));
        assertTrue(response.getContentAsString().contains("\"status\":429"));
        assertTrue(response.getContentAsString().contains("Retry after 30 seconds"));
    }

    @Test
    void blocksWebRequestWithPlainTextBodyWhenLimitExceeded() throws Exception {
        MockHttpServletRequest request = requestFor("/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        MockFilterChain chain = new MockFilterChain();
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(false);
        when(rateLimitService.getTtl(anyString())).thenReturn(15L);

        filter.doFilter(request, response, chain);

        assertEquals(429, response.getStatus());
        assertTrue(response.getContentType().startsWith("text/plain"));
        assertTrue(response.getContentAsString().contains("Retry after 15 seconds"));
    }

    @Test
    void keysAnonymousRequestsByIp() throws Exception {
        MockHttpServletRequest request = requestFor("/api/products");
        request.setRemoteAddr("203.0.113.5");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        verify(rateLimitService).isAllowed(eq("203.0.113.5:/api/products"), eq(100), eq(60));
    }

    @Test
    void keysAuthenticatedRequestsByUserId() throws Exception {
        authenticateAs(42L);
        MockHttpServletRequest request = requestFor("/api/products");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.isAllowed(anyString(), anyInt(), anyInt())).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        verify(rateLimitService).isAllowed(eq("user:42:/api/products"), eq(100), eq(60));
    }

    @Test
    void honorsExplicitRateLimitAnnotationOnHandlerMethod() throws Exception {
        Method method = AnnotatedController.class.getMethod("stats");
        HandlerMethod handlerMethod = new HandlerMethod(new AnnotatedController(), method);
        when(handlerMapping.getHandler(any())).thenReturn(new HandlerExecutionChain(handlerMethod));

        MockHttpServletRequest request = requestFor("/api/audit/stats");
        request.setRemoteAddr("203.0.113.9");
        MockHttpServletResponse response = new MockHttpServletResponse();
        when(rateLimitService.isAllowed(anyString(), eq(10), eq(60))).thenReturn(true);

        filter.doFilter(request, response, new MockFilterChain());

        verify(rateLimitService, times(1)).isAllowed(eq("203.0.113.9:/api/audit/stats"), eq(10), eq(60));
        verify(rateLimitConfig, times(0)).getSettingsForEndpoint(anyString());
    }

    private MockHttpServletRequest requestFor(String uri) {
        MockHttpServletRequest request = new MockHttpServletRequest("GET", uri);
        request.setAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE, uri);
        return request;
    }

    private void authenticateAs(long userId) {
        CustomUserPrincipal principal = new CustomUserPrincipal(userId, "user" + userId + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), true);
        Authentication authentication = new UsernamePasswordAuthenticationToken(principal, null,
                principal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(authentication);
    }

    static class AnnotatedController {
        @RateLimit(requests = 10, window = 60, perUser = false)
        public String stats() {
            return "stats";
        }
    }
}
