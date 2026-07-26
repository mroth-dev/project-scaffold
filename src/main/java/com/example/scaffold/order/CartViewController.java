package com.example.scaffold.order;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

import com.example.scaffold.product.ProductVariation;
import com.example.scaffold.product.ProductVariationRepository;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

@Controller
@RequestMapping("/cart")
public class CartViewController {

    private final ShoppingCart cart;
    private final ProductVariationRepository productVariationRepository;
    private final OrderService orderService;

    public CartViewController(ShoppingCart cart, ProductVariationRepository productVariationRepository,
            OrderService orderService) {
        this.cart = cart;
        this.productVariationRepository = productVariationRepository;
        this.orderService = orderService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("lines", buildCartLines());
        model.addAttribute("total", cartTotal());
        return "cart/index";
    }

    @PostMapping("/items")
    @ResponseBody
    public String addItem(@RequestParam Long variationId, @RequestParam(defaultValue = "1") int quantity) {
        cart.add(variationId, quantity);
        return String.valueOf(cart.getItemCount());
    }

    @PostMapping("/items/{variationId}/remove")
    public String removeItem(@PathVariable Long variationId, Model model) {
        cart.remove(variationId);
        model.addAttribute("lines", buildCartLines());
        model.addAttribute("total", cartTotal());
        return "cart/fragments/contents :: cartContents";
    }

    @PostMapping("/checkout")
    public String checkout(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        if (principal == null) {
            return "redirect:/login";
        }
        if (cart.isEmpty()) {
            return "redirect:/cart";
        }

        List<OrderItemRequest> items = cart.getItems().entrySet().stream()
                .map(entry -> new OrderItemRequest(entry.getKey(), entry.getValue()))
                .toList();
        OrderDto order = orderService.createOrder(principal.getId(), new OrderRequest(items));
        cart.clear();
        return "redirect:/orders/" + order.id();
    }

    private List<CartLine> buildCartLines() {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
            productVariationRepository.findById(entry.getKey()).ifPresent(variation -> {
                BigDecimal unitPrice = variation.getProduct().getBasePrice().add(variation.getPriceAdjustment());
                lines.add(new CartLine(variation, entry.getValue(), unitPrice, unitPrice.multiply(BigDecimal.valueOf(entry.getValue()))));
            });
        }
        return lines;
    }

    private BigDecimal cartTotal() {
        return buildCartLines().stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record CartLine(ProductVariation variation, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
