package com.example.scaffold.category;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import com.example.scaffold.product.ProductService;

/**
 * Storefront category browsing, keyed by slug rather than id so URLs read as
 * {@code /categories/polos} instead of an opaque numeric id.
 */
@Controller
@RequestMapping("/categories")
public class CategoryViewController {

    private final CategoryService categoryService;
    private final ProductService productService;

    public CategoryViewController(CategoryService categoryService, ProductService productService) {
        this.categoryService = categoryService;
        this.productService = productService;
    }

    @GetMapping("/{slug}")
    public String show(@PathVariable String slug, Pageable pageable, Model model) {
        CategoryDto category = categoryService.getCategoryBySlug(slug);
        populateResults(category, pageable, model);
        model.addAttribute("category", category);
        model.addAttribute("breadcrumb", categoryService.getAncestors(category.id()));
        model.addAttribute("subcategories", categoryService.getChildren(category.id()));
        return "categories/show";
    }

    @GetMapping("/{slug}/list")
    public String list(@PathVariable String slug, Pageable pageable, Model model) {
        populateResults(categoryService.getCategoryBySlug(slug), pageable, model);
        return "products/fragments/results :: productResults";
    }

    private void populateResults(CategoryDto category, Pageable pageable, Model model) {
        model.addAttribute("page", productService.getProductsByCategory(category.id(), pageable));
        model.addAttribute("query", null);
        model.addAttribute("basePath", "/categories/" + category.slug());
    }
}
