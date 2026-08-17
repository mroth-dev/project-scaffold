package com.example.scaffold.order;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.config.RateLimitConfig;
import com.example.scaffold.config.RateLimitService;
import com.example.scaffold.config.SecurityConfig;
import com.example.scaffold.security.CustomUserDetailsService;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;
import com.example.scaffold.security.JwtTokenProvider;
import com.example.scaffold.user.Address;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(OrderRestController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
class OrderRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

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

    private Authentication admin(long id) {
        CustomUserPrincipal principal = new CustomUserPrincipal(id, "admin" + id + "@example.com", "hash",
                List.of(new SimpleGrantedAuthority("ROLE_ADMIN")), true);
        return new UsernamePasswordAuthenticationToken(principal, null, principal.getAuthorities());
    }

    private OrderDto sampleOrder(Long id, Long customerId) {
        OrderItemDto item = new OrderItemDto(1L, 1L, "T-Shirt", "TSHIRT-001-M-WHITE", 2, new BigDecimal("19.99"),
                new BigDecimal("39.98"));
        return new OrderDto(id, customerId, OrderStatus.PENDING, new BigDecimal("39.98"), null, BigDecimal.ZERO,
                new Address(), List.of(item), LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void createOrderReturns201() throws Exception {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));
        given(orderService.createOrder(eq(1L), any(OrderRequest.class))).willReturn(sampleOrder(10L, 1L));

        mockMvc.perform(post("/api/orders")
                        .with(authentication(customer(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(10));
    }

    @Test
    void createOrderReturns401WhenUnauthenticated() throws Exception {
        OrderRequest request = new OrderRequest(List.of(new OrderItemRequest(1L, 2)));

        mockMvc.perform(post("/api/orders")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void ownerCanReadTheirOwnOrder() throws Exception {
        given(orderService.getOrder(10L)).willReturn(sampleOrder(10L, 1L));

        mockMvc.perform(get("/api/orders/10").with(authentication(customer(1L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.customerId").value(1));
    }

    @Test
    void otherCustomerCannotReadSomeoneElsesOrder() throws Exception {
        given(orderService.getOrder(10L)).willReturn(sampleOrder(10L, 1L));

        mockMvc.perform(get("/api/orders/10").with(authentication(customer(2L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void adminCanReadAnyOrder() throws Exception {
        given(orderService.getOrder(10L)).willReturn(sampleOrder(10L, 1L));

        mockMvc.perform(get("/api/orders/10").with(authentication(admin(99L))))
                .andExpect(status().isOk());
    }

    @Test
    void listAllOrdersRequiresStaffRole() throws Exception {
        mockMvc.perform(get("/api/orders").with(authentication(customer(1L))))
                .andExpect(status().isForbidden());
    }

    @Test
    void listAllOrdersAllowedForAdmin() throws Exception {
        given(orderService.getOrders(eq(null), any()))
                .willReturn(new PageImpl<>(List.of(sampleOrder(10L, 1L)), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/orders").with(authentication(admin(99L))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].id").value(10));
    }

    @Test
    void updateStatusRequiresStaffRole() throws Exception {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);

        mockMvc.perform(put("/api/orders/10/status")
                        .with(authentication(customer(1L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isForbidden());
    }

    @Test
    void updateStatusAllowedForAdmin() throws Exception {
        OrderStatusUpdateRequest request = new OrderStatusUpdateRequest(OrderStatus.CONFIRMED);
        given(orderService.updateOrderStatus(eq(10L), eq(OrderStatus.CONFIRMED)))
                .willReturn(new OrderDto(10L, 1L, OrderStatus.CONFIRMED, new BigDecimal("39.98"), null,
                        BigDecimal.ZERO, new Address(), List.of(), LocalDateTime.now(), LocalDateTime.now()));

        mockMvc.perform(put("/api/orders/10/status")
                        .with(authentication(admin(99L)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("CONFIRMED"));
    }
}
