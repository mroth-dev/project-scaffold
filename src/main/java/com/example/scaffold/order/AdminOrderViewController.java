package com.example.scaffold.order;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

/**
 * Server-rendered order management for the admin section. Unlike
 * {@link OrderViewController}, which only lets a customer view their own
 * orders, this exposes every order to ADMIN/MANAGER staff.
 */
@Controller
@RequestMapping("/admin/orders")
public class AdminOrderViewController {

    private final OrderService orderService;

    public AdminOrderViewController(OrderService orderService) {
        this.orderService = orderService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) OrderStatus status, Pageable pageable, Model model) {
        model.addAttribute("page", orderService.getOrders(status, pageable));
        model.addAttribute("status", status);
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders/index";
    }

    @GetMapping("/{orderId}")
    public String detail(@PathVariable Long orderId, Model model) {
        model.addAttribute("order", orderService.getOrder(orderId));
        model.addAttribute("statuses", OrderStatus.values());
        return "admin/orders/detail";
    }

    @PostMapping("/{orderId}/status")
    public String updateStatus(@PathVariable Long orderId, @RequestParam OrderStatus status) {
        orderService.updateOrderStatus(orderId, status);
        return "redirect:/admin/orders/" + orderId;
    }
}
