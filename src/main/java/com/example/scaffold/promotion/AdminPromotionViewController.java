package com.example.scaffold.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Server-rendered promotion management for the admin section. Delegates to
 * {@link PromotionService} for create/update/delete and activation toggling;
 * eligibility rules (dates, usage limits, minimum spend) live in the service
 * so both this and the cart checkout flow share the same validation.
 */
@Controller
@RequestMapping("/admin/promotions")
public class AdminPromotionViewController {

    private final PromotionService promotionService;

    public AdminPromotionViewController(PromotionService promotionService) {
        this.promotionService = promotionService;
    }

    @GetMapping
    public String index(Model model) {
        model.addAttribute("promotions", promotionService.getPromotions());
        return "admin/promotions/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("types", PromotionType.values());
        return "admin/promotions/form";
    }

    @GetMapping("/{promotionId}/edit")
    public String editForm(@PathVariable Long promotionId, Model model) {
        model.addAttribute("promotion", promotionService.getPromotion(promotionId));
        model.addAttribute("types", PromotionType.values());
        return "admin/promotions/form";
    }

    @PostMapping
    public String create(@ModelAttribute PromotionFormParams form) {
        promotionService.createPromotion(form.toRequest());
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{promotionId}")
    public String update(@PathVariable Long promotionId, @ModelAttribute PromotionFormParams form) {
        promotionService.updatePromotion(promotionId, form.toRequest());
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{promotionId}/activate")
    public String activate(@PathVariable Long promotionId) {
        promotionService.setActive(promotionId, true);
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{promotionId}/deactivate")
    public String deactivate(@PathVariable Long promotionId) {
        promotionService.setActive(promotionId, false);
        return "redirect:/admin/promotions";
    }

    @PostMapping("/{promotionId}/delete")
    public String delete(@PathVariable Long promotionId) {
        promotionService.deletePromotion(promotionId);
        return "redirect:/admin/promotions";
    }

    /** Binds the promotion form's fields, submitted as strings so blank inputs bind cleanly. */
    public record PromotionFormParams(
            String name,
            String code,
            String description,
            PromotionType type,
            String value,
            String startDate,
            String endDate,
            String usageLimit,
            String usageLimitPerCustomer,
            String minimumPurchaseAmount,
            boolean active) {

        PromotionRequest toRequest() {
            return new PromotionRequest(
                    name,
                    code,
                    description,
                    type,
                    parseDecimal(value),
                    parseDateTime(startDate),
                    parseDateTime(endDate),
                    parseInt(usageLimit),
                    parseInt(usageLimitPerCustomer),
                    parseDecimal(minimumPurchaseAmount),
                    active);
        }

        private static LocalDateTime parseDateTime(String value) {
            return value == null || value.isBlank() ? null : LocalDateTime.parse(value);
        }

        private static Integer parseInt(String value) {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        }

        private static BigDecimal parseDecimal(String value) {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        }
    }
}
