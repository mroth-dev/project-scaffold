package com.example.scaffold.order;

import java.net.URI;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
public class OrderRestController {

    private final OrderService orderService;

    public OrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public ResponseEntity<OrderDto> createOrder(@AuthenticationPrincipal CustomUserPrincipal principal,
            @Valid @RequestBody OrderRequest request) {
        OrderDto created = orderService.createOrder(principal.getId(), request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @GetMapping("/me")
    public Page<OrderDto> getMyOrders(@AuthenticationPrincipal CustomUserPrincipal principal, Pageable pageable) {
        return orderService.getOrdersForCustomer(principal.getId(), pageable);
    }

    @GetMapping("/{orderId}")
    public OrderDto getOrder(Authentication authentication, @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long orderId) {
        OrderDto order = orderService.getOrder(orderId);
        if (!isPrivileged(authentication) && !order.customerId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        return order;
    }

    @GetMapping
    public Page<OrderDto> getOrders(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
        return orderService.getOrders(status, pageable);
    }

    @PutMapping("/{orderId}/status")
    public OrderDto updateStatus(@PathVariable Long orderId, @Valid @RequestBody OrderStatusUpdateRequest request) {
        return orderService.updateOrderStatus(orderId, request.status());
    }

    private boolean isPrivileged(Authentication authentication) {
        for (GrantedAuthority authority : authentication.getAuthorities()) {
            String role = authority.getAuthority();
            if (role.equals("ROLE_ADMIN") || role.equals("ROLE_MANAGER")) {
                return true;
            }
        }
        return false;
    }
}
