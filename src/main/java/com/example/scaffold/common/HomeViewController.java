package com.example.scaffold.common;

import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

import com.example.scaffold.category.CategoryService;
import com.example.scaffold.product.ProductService;

@Controller
public class HomeViewController {

    private static final int CAROUSEL_SIZE = 5;

    private final ProductService productService;
    private final CategoryService categoryService;

    public HomeViewController(ProductService productService, CategoryService categoryService) {
        this.productService = productService;
        this.categoryService = categoryService;
    }

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("carouselProducts",
                productService.searchProducts(null, PageRequest.of(0, CAROUSEL_SIZE)).getContent());
        model.addAttribute("categories", categoryService.getCategoryTree());
        return "home";
    }
}
