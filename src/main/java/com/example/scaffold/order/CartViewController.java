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

import com.example.scaffold.exception.BusinessException;
import com.example.scaffold.product.ProductVariation;
import com.example.scaffold.product.ProductVariationRepository;
import com.example.scaffold.promotion.PromotionQuote;
import com.example.scaffold.promotion.PromotionService;
import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;
import com.example.scaffold.user.Address;
import com.example.scaffold.user.UserService;

@Controller
@RequestMapping("/cart")
public class CartViewController {

    private final ShoppingCart cart;
    private final ProductVariationRepository productVariationRepository;
    private final OrderService orderService;
    private final PromotionService promotionService;
    private final UserService userService;

    public CartViewController(ShoppingCart cart, ProductVariationRepository productVariationRepository,
            OrderService orderService, PromotionService promotionService, UserService userService) {
        this.cart = cart;
        this.productVariationRepository = productVariationRepository;
        this.orderService = orderService;
        this.promotionService = promotionService;
        this.userService = userService;
    }

    @GetMapping
    public String index(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        populateCartModel(model, null, principal);
        return "cart/index";
    }

    @PostMapping("/items")
    @ResponseBody
    public String addItem(@RequestParam Long variationId, @RequestParam(defaultValue = "1") int quantity) {
        cart.add(variationId, quantity);
        return String.valueOf(cart.getItemCount());
    }

    @PostMapping("/items/{variationId}/remove")
    public String removeItem(@AuthenticationPrincipal CustomUserPrincipal principal, @PathVariable Long variationId,
            Model model) {
        cart.remove(variationId);
        populateCartModel(model, null, principal);
        return "cart/fragments/contents :: cartContents";
    }

    @PostMapping("/promotion")
    public String applyPromotion(@AuthenticationPrincipal CustomUserPrincipal principal, @RequestParam String code,
            Model model) {
        BigDecimal subtotal = cartSubtotal(buildCartLines());
        try {
            promotionService.quote(code, subtotal);
            cart.setPromotionCode(code.trim().toUpperCase());
            populateCartModel(model, null, principal);
        } catch (BusinessException ex) {
            populateCartModel(model, ex.getMessage(), principal);
        }
        return "cart/fragments/contents :: cartContents";
    }

    @PostMapping("/promotion/remove")
    public String removePromotion(@AuthenticationPrincipal CustomUserPrincipal principal, Model model) {
        cart.setPromotionCode(null);
        populateCartModel(model, null, principal);
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
        if (!userService.hasCompleteShippingAddress(principal.getId())) {
            return "redirect:/account";
        }

        List<OrderItemRequest> items = cart.getItems().entrySet().stream()
                .map(entry -> new OrderItemRequest(entry.getKey(), entry.getValue()))
                .toList();
        OrderDto order = orderService.createOrder(principal.getId(), new OrderRequest(items, cart.getPromotionCode()));
        cart.clear();
        return "redirect:/orders/" + order.id();
    }

    private void populateCartModel(Model model, String forcedPromotionError, CustomUserPrincipal principal) {
        List<CartLine> lines = buildCartLines();
        BigDecimal subtotal = cartSubtotal(lines);

        BigDecimal discount = BigDecimal.ZERO;
        String promotionError = forcedPromotionError;
        PromotionQuote promotion = null;
        if (cart.getPromotionCode() != null) {
            try {
                promotion = promotionService.quote(cart.getPromotionCode(), subtotal);
                discount = promotion.discountAmount();
            } catch (BusinessException ex) {
                promotionError = ex.getMessage();
            }
        }

        Address shippingAddress = principal != null ? userService.getAccount(principal.getId()).address() : null;
        model.addAttribute("lines", lines);
        model.addAttribute("subtotal", subtotal);
        model.addAttribute("discount", discount);
        model.addAttribute("total", subtotal.subtract(discount));
        model.addAttribute("promotion", promotion);
        model.addAttribute("shippingAddress", shippingAddress);
        model.addAttribute("hasShippingAddress", shippingAddress != null && shippingAddress.isComplete());
        model.addAttribute("promotionCode", cart.getPromotionCode());
        model.addAttribute("promotionError", promotionError);
    }

    private List<CartLine> buildCartLines() {
        List<CartLine> lines = new ArrayList<>();
        for (Map.Entry<Long, Integer> entry : cart.getItems().entrySet()) {
            productVariationRepository.findById(entry.getKey()).ifPresent(variation -> {
                BigDecimal unitPrice = variation.getProduct().getBasePrice().add(variation.getPriceAdjustment());
                BigDecimal lineTotal = unitPrice.multiply(BigDecimal.valueOf(entry.getValue()));
                lines.add(new CartLine(variation, entry.getValue(), unitPrice, lineTotal));
            });
        }
        return lines;
    }

    private BigDecimal cartSubtotal(List<CartLine> lines) {
        return lines.stream().map(CartLine::subtotal).reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    public record CartLine(ProductVariation variation, int quantity, BigDecimal unitPrice, BigDecimal subtotal) {
    }
}
