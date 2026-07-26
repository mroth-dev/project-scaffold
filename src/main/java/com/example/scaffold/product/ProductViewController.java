package com.example.scaffold.product;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/products")
public class ProductViewController {

    private final ProductService productService;

    public ProductViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String query, Pageable pageable, Model model) {
        model.addAttribute("page", productService.searchProducts(query, pageable));
        model.addAttribute("query", query);
        return "products/index";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String query, Pageable pageable, Model model) {
        model.addAttribute("page", productService.searchProducts(query, pageable));
        model.addAttribute("query", query);
        return "products/fragments/results :: productResults";
    }

    @GetMapping("/{productId}")
    public String detail(@PathVariable Long productId, Model model) {
        model.addAttribute("product", productService.getProduct(productId));
        return "products/detail";
    }
}
