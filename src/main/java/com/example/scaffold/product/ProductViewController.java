package com.example.scaffold.product;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.example.scaffold.category.CategoryService;

@Controller
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService productService;
    private final CategoryService categoryService;

    public ProductViewController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId, Pageable pageable, Model model) {
        populateResults(query, categoryId, pageable, model);
        if (categoryId != null) {
            model.addAttribute("categoryName", categoryService.getCategory(categoryId).name());
        }
        return "products/index";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId, Pageable pageable, Model model) {
        populateResults(query, categoryId, pageable, model);
        return "products/fragments/results :: productResults";
    }

    private void populateResults(String query, Long categoryId, Pageable pageable, Model model) {
        Page<ProductDto> page = categoryId != null
                ? productService.getProductsByCategory(categoryId, pageable)
                : productService.searchProducts(query, pageable);
        model.addAttribute("page", page);
        model.addAttribute("query", query);
        model.addAttribute("categoryId", categoryId);
    }

    @GetMapping("/{productId}")
    public String detail(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProduct(productId));
        return "products/detail";
    }
}
