package com.example.scaffold.category;

import java.net.URI;
import java.time.Duration;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import com.example.scaffold.product.ProductSummaryDto;

import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
public class CategoryRestController {

    private static final CacheControl CATEGORY_CACHE_CONTROL = CacheControl.maxAge(Duration.ofMinutes(30)).cachePublic();

    private final CategoryService categoryService;

    public CategoryRestController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getCategoryTree());
    }

    @GetMapping("/{categoryId}")
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getCategory(categoryId));
    }

    @GetMapping("/{categoryId}/children")
    public ResponseEntity<List<CategoryDto>> getChildren(@PathVariable Long categoryId) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getChildren(categoryId));
    }

    @GetMapping("/{categoryId}/products")
    public ResponseEntity<Page<ProductSummaryDto>> getProducts(@PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getProductsInCategory(categoryId, pageable));
    }

    @PostMapping
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryDto created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{categoryId}")
    public CategoryDto updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<Void> addProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        categoryService.addProductToCategory(categoryId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/products/{productId}")
    public ResponseEntity<Void> removeProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        categoryService.removeProductFromCategory(categoryId, productId);
        return ResponseEntity.noContent().build();
    }
}
