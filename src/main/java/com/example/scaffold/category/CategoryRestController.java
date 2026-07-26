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

import com.example.scaffold.common.ApiError;
import com.example.scaffold.product.ProductSummaryDto;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/categories")
@Tag(name = "Categories", description = "Hierarchical product categories")
public class CategoryRestController {

    private static final CacheControl CATEGORY_CACHE_CONTROL =
            CacheControl.maxAge(Duration.ofMinutes(30)).cachePublic();

    private final CategoryService categoryService;

    public CategoryRestController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/tree")
    @Operation(summary = "Get the full category tree", description = "Root categories with nested children")
    public ResponseEntity<List<CategoryTreeDto>> getCategoryTree() {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getCategoryTree());
    }

    @GetMapping("/{categoryId}")
    @Operation(summary = "Get a category by id")
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<CategoryDto> getCategory(@PathVariable Long categoryId) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getCategory(categoryId));
    }

    @GetMapping("/{categoryId}/children")
    @Operation(summary = "List the direct children of a category")
    public ResponseEntity<List<CategoryDto>> getChildren(@PathVariable Long categoryId) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL).body(categoryService.getChildren(categoryId));
    }

    @GetMapping("/{categoryId}/products")
    @Operation(summary = "List products assigned to a category")
    public ResponseEntity<Page<ProductSummaryDto>> getProducts(@PathVariable Long categoryId, Pageable pageable) {
        return ResponseEntity.ok().cacheControl(CATEGORY_CACHE_CONTROL)
                .body(categoryService.getProductsInCategory(categoryId, pageable));
    }

    @PostMapping
    @Operation(summary = "Create a category")
    @ApiResponse(responseCode = "201", description = "Category created")
    @ApiResponse(responseCode = "400", description = "Validation failed or invalid parent",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<CategoryDto> createCategory(@Valid @RequestBody CategoryRequest request) {
        CategoryDto created = categoryService.createCategory(request);
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();
        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{categoryId}")
    @Operation(summary = "Update a category")
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public CategoryDto updateCategory(@PathVariable Long categoryId, @Valid @RequestBody CategoryRequest request) {
        return categoryService.updateCategory(categoryId, request);
    }

    @DeleteMapping("/{categoryId}")
    @Operation(summary = "Delete a category")
    @ApiResponse(responseCode = "204", description = "Category deleted")
    @ApiResponse(responseCode = "404", description = "Category not found",
            content = @Content(schema = @Schema(implementation = ApiError.class)))
    public ResponseEntity<Void> deleteCategory(@PathVariable Long categoryId) {
        categoryService.deleteCategory(categoryId);
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{categoryId}/products/{productId}")
    @Operation(summary = "Assign a product to a category")
    public ResponseEntity<Void> addProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        categoryService.addProductToCategory(categoryId, productId);
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{categoryId}/products/{productId}")
    @Operation(summary = "Remove a product from a category")
    public ResponseEntity<Void> removeProduct(@PathVariable Long categoryId, @PathVariable Long productId) {
        categoryService.removeProductFromCategory(categoryId, productId);
        return ResponseEntity.noContent().build();
    }
}
