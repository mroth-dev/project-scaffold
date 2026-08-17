package com.example.scaffold.product;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

/**
 * Products sorted newest-first. Reuses {@link ProductService#searchProducts}
 * with a blank query - that path already delegates to a plain, Pageable-sorted
 * findAll - so the createdAt-desc default below is all that's needed.
 */
@Controller
@RequestMapping("/new-in")
public class NewInViewController {

    private final ProductService productService;

    public NewInViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String index(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        populateResults(pageable, model);
        model.addAttribute("title", "New In");
        return "products/listing";
    }

    @GetMapping("/list")
    public String list(
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable,
            Model model) {
        populateResults(pageable, model);
        return "products/fragments/results :: productResults";
    }

    private void populateResults(Pageable pageable, Model model) {
        model.addAttribute("page", productService.searchProducts(null, pageable));
        model.addAttribute("query", null);
        model.addAttribute("basePath", "/new-in");
    }
}
