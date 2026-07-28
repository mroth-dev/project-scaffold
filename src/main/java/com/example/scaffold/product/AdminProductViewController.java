package com.example.scaffold.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.scaffold.category.CategoryService;

/**
 * Server-rendered product management for the admin section. Delegates to
 * {@link ProductService} for the same create/update/delete logic the REST
 * API uses; variations are edited as a whole list alongside the product,
 * matching {@link ProductService#updateProduct} replace-on-save semantics.
 */
@Controller
@RequestMapping("/admin/products")
public class AdminProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public AdminProductViewController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String query, Pageable pageable, Model model) {
        model.addAttribute("page", productService.searchProducts(query, pageable));
        model.addAttribute("query", query);
        return "admin/products/index";
    }

    @GetMapping("/low-stock")
    public String lowStock(Pageable pageable, Model model) {
        model.addAttribute("page", productService.getLowStockItems(pageable));
        return "admin/products/low-stock";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("categoryOptions", categoryService.getFlattenedOptions());
        return "admin/products/form";
    }

    @GetMapping("/{productId}/edit")
    public String editForm(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProduct(productId));
        model.addAttribute("categoryOptions", categoryService.getFlattenedOptions());
        return "admin/products/form";
    }

    @PostMapping
    public String create(@ModelAttribute ProductFormParams form) {
        productService.createProduct(form.toRequest(List.of()));
        return "redirect:/admin/products";
    }

    @PostMapping("/{productId}")
    public String update(@PathVariable Long productId, @ModelAttribute ProductFormParams form) {
        List<ProductImageRequest> existingImages = productService.getProduct(productId).images().stream()
                .map(image -> new ProductImageRequest(image.url(), image.thumbnailUrl(), image.altText(),
                        image.sortOrder()))
                .toList();
        productService.updateProduct(productId, form.toRequest(existingImages));
        return "redirect:/admin/products";
    }

    @PostMapping("/{productId}/delete")
    public String delete(@PathVariable Long productId) {
        productService.deleteProduct(productId);
        return "redirect:/admin/products";
    }

    /**
     * Binds the product form's flat fields plus the parallel variation-row
     * arrays submitted by the repeatable variations table.
     */
    public record ProductFormParams(
            String name,
            String description,
            String sku,
            BigDecimal rrp,
            BigDecimal basePrice,
            boolean active,
            List<String> variationSize,
            List<String> variationColor,
            List<String> variationSku,
            List<String> variationInventory,
            List<String> variationPriceAdjustment,
            List<String> categoryIds) {

        ProductRequest toRequest(List<ProductImageRequest> images) {
            return new ProductRequest(
                    name, description, sku, rrp, basePrice, active, images, toVariations(), toCategoryIds());
        }

        private List<Long> toCategoryIds() {
            if (categoryIds == null) {
                return List.of();
            }
            return categoryIds.stream()
                    .filter(id -> id != null && !id.isBlank())
                    .map(Long::valueOf)
                    .toList();
        }

        private List<ProductVariationRequest> toVariations() {
            List<ProductVariationRequest> variations = new ArrayList<>();
            int rows = variationSku == null ? 0 : variationSku.size();
            for (int i = 0; i < rows; i++) {
                String sku = at(variationSku, i);
                if (sku == null || sku.isBlank()) {
                    continue;
                }
                Integer inventoryCount = parseInt(at(variationInventory, i));
                BigDecimal priceAdjustment = parseDecimal(at(variationPriceAdjustment, i));
                variations.add(new ProductVariationRequest(
                        at(variationSize, i), at(variationColor, i), sku, inventoryCount, priceAdjustment));
            }
            return variations;
        }

        private static String at(List<String> values, int index) {
            return values != null && index < values.size() ? values.get(index) : null;
        }

        private static Integer parseInt(String value) {
            return value == null || value.isBlank() ? null : Integer.valueOf(value.trim());
        }

        private static BigDecimal parseDecimal(String value) {
            return value == null || value.isBlank() ? null : new BigDecimal(value.trim());
        }
    }
}
