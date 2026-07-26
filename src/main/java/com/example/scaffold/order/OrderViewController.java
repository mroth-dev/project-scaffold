package com.example.scaffold.order;

import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

@Controller
@RequestMapping("/orders")
public class OrderViewController {

    private final OrderService orderService;

    public OrderViewController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserPrincipal principal, Pageable pageable, Model model) {
        model.addAttribute("page", orderService.getOrdersForCustomer(principal.getId(), pageable));
        return "orders/index";
    }

    @GetMapping("/{orderId}")
    public String detail(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long orderId,
            Model model) {
        OrderDto order = orderService.getOrder(orderId);
        if (!order.customerId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        model.addAttribute("order", order);
        return "orders/detail";
    }

    @GetMapping("/{orderId}/status")
    public String status(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long orderId,
            Model model) {
        OrderDto order = orderService.getOrder(orderId);
        if (!order.customerId().equals(principal.getId())) {
            throw new AccessDeniedException("You do not have access to this order");
        }
        model.addAttribute("order", order);
        return "orders/fragments/status-badge :: statusBadge";
    }
}
