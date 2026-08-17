package com.example.scaffold.common;

import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.model;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.config.RateLimitConfig;
import com.example.scaffold.config.RateLimitService;
import com.example.scaffold.config.SecurityConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.order.OrderDto;
import com.example.scaffold.order.OrderService;
import com.example.scaffold.order.OrderStatus;
import com.example.scaffold.order.OrderViewController;
import com.example.scaffold.product.ProductService;
import com.example.scaffold.product.ProductViewController;
import com.example.scaffold.security.CustomUserDetailsService;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;
import com.example.scaffold.security.JwtTokenProvider;
import com.example.scaffold.user.Address;

@WebMvcTest({ProductViewController.class, OrderViewController.class})
@Import({SecurityConfig.class, JwtTokenProvider.class})
class WebExceptionHandlerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private OrderService orderService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // RateLimitingFilter sits in the security chain; rateLimitConfig.isEnabled()
    // defaults to false when mocked, so it's a no-op here.
    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    private Authentication customer(long id) {
        CustomUserPrincipal principal = new CustomUserPrincipal(id, "customer" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_CUSTOMER")), true);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private OrderDto sampleOrder(Long id, Long customerId) {
        return new OrderDto(id, customerId, OrderStatus.PENDING, new BigDecimal("10.00"), null, BigDecimal.ZERO,
                new Address(), List.of(), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void notFoundRendersErrorViewWith404() throws Exception {
        given(productService.getProduct(99L)).willThrow(new NotFoundException("Product", 99L));

        mockMvc.perform(get("/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 404))
                .andExpect(model().attribute("message", "Product not found with id: 99"));
    }

    @Test
    void accessDeniedRendersErrorViewWith403() throws Exception {
        given(orderService.getOrder(10L)).willReturn(sampleOrder(10L, 1L));

        mockMvc.perform(get("/orders/10").with(authentication(customer(2L))))
                .andExpect(status().isForbidden())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("status", 403));
    }

    @Test
    void unexpectedExceptionRendersGenericErrorViewWithoutLeakingDetails() throws Exception {
        given(productService.getProduct(7L))
                .willThrow(new RuntimeException("password=hunter2 while querying products"));

        mockMvc.perform(get("/products/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(view().name("error"))
                .andExpect(model().attribute("message", "An unexpected error occurred. Please try again later."));
    }
}
