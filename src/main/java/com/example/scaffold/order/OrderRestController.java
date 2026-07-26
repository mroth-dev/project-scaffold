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

import com.example.scaffold.common.ApiError;
import com.example.scaffold.config.OpenApiConfig;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Orders", description = "Order placement, tracking and status management (requires authentication)")
@SecurityRequirement(name = OpenApiConfig.BEARER_AUTH)
public class OrderRestController {

    private final OrderService orderService;

    public OrderRestController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    @Operation(summary = "Place an order for the authenticated customer")
    @ApiResponse(responseCode = "201", description = "Order created")
    @ApiResponse(responseCode = "409", description = "Insufficient inventory for a requested item",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
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
    @Operation(summary = "List the authenticated customer's own orders")
    public Page<OrderDto> getMyOrders(@AuthenticationPrincipal CustomUserPrincipal principal, Pageable pageable) {
        return orderService.getOrdersForCustomer(principal.getId(), pageable);
    }

    @GetMapping("/{orderId}")
    @Operation(summary = "Get an order by id",
            description = "Customers may only fetch their own orders; ADMIN/MANAGER may fetch any order")
    @ApiResponse(responseCode = "403", description = "Not the order owner and not staff",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    @ApiResponse(responseCode = "404", description = "Order not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public OrderDto getOrder(Authentication authentication, @AuthenticationPrincipal CustomUserPrincipal principal,
            @PathVariable Long orderId) {
        OrderDto order = orderService.getOrder(orderId);
        if (!isPrivileged(authentication) && !order.customerId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        return order;
    }

    @GetMapping
    @Operation(summary = "List all orders", description = "Restricted to ADMIN/MANAGER")
    public Page<OrderDto> getOrders(@RequestParam(required = false) OrderStatus status, Pageable pageable) {
        return orderService.getOrders(status, pageable);
    }

    @PutMapping("/{orderId}/status")
    @Operation(summary = "Update an order's status", description = "Restricted to ADMIN/MANAGER")
    @ApiResponse(responseCode = "409", description = "Order is already in a terminal status",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
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
