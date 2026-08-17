package com.example.scaffold.product;

import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/search")
public class SearchViewController {

    private final ProductService productService;

    public SearchViewController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public String index(@RequestParam(required = false) String query, Pageable pageable, Model model) {
        populateResults(query, pageable, model);
        model.addAttribute("title", query != null && !query.isBlank() ? "Search: \"" + query + "\"" : "Search");
        return "products/listing";
    }

    @GetMapping("/list")
    public String list(@RequestParam(required = false) String query, Pageable pageable, Model model) {
        populateResults(query, pageable, model);
        return "products/fragments/results :: productResults";
    }

    private void populateResults(String query, Pageable pageable, Model model) {
        model.addAttribute("page", productService.searchProducts(query, pageable));
        model.addAttribute("query", query);
        model.addAttribute("basePath", "/search");
    }
}
