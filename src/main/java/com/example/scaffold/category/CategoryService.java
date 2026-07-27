package com.example.scaffold.category;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.product.Product;
import com.example.scaffold.product.ProductRepository;
import com.example.scaffold.product.ProductSummaryDto;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;
    private final ProductRepository productRepository;

    public CategoryService(CategoryRepository categoryRepository, ProductRepository productRepository) {
        this.categoryRepository = categoryRepository;
        this.productRepository = productRepository;
    }

    @Cacheable(value = CacheConfig.CATEGORY_CACHE, key = "#id")
    public CategoryDto getCategory(Long id) {
        log.debug("Fetching category by id: {}", id);
        return toDto(findCategoryOrThrow(id));
    }

    @Cacheable(value = CacheConfig.CATEGORY_CACHE, key = "'tree'")
    public List<CategoryTreeDto> getCategoryTree() {
        log.debug("Building category tree");
        List<Category> all = categoryRepository.findAll();
        Map<Long, List<Category>> childrenByParentId = all.stream()
                .filter(category -> category.getParent() != null)
                .collect(Collectors.groupingBy(category -> category.getParent().getId()));

        return all.stream()
                .filter(category -> category.getParent() == null)
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .map(root -> toTreeDto(root, childrenByParentId))
                .collect(Collectors.toCollection(ArrayList::new));
    }

    public List<CategoryDto> getChildren(Long parentId) {
        log.debug("Fetching children of category: {}", parentId);
        return categoryRepository.findByParentIdOrderBySortOrderAsc(parentId).stream()
                .map(this::toDto)
                .toList();
    }

    public List<CategoryOption> getFlattenedOptions() {
        return flattenOptions(getCategoryTree(), 0);
    }

    private List<CategoryOption> flattenOptions(List<CategoryTreeDto> nodes, int depth) {
        List<CategoryOption> options = new ArrayList<>();
        for (CategoryTreeDto node : nodes) {
            options.add(new CategoryOption(node.id(), "— ".repeat(depth) + node.name()));
            options.addAll(flattenOptions(node.children(), depth + 1));
        }
        return options;
    }

    public Page<ProductSummaryDto> getProductsInCategory(Long categoryId, Pageable pageable) {
        log.debug("Fetching products in category: {}", categoryId);
        return productRepository.findByCategoriesId(categoryId, pageable).map(this::toProductSummaryDto);
    }

    public CategoryDto createCategory(CategoryRequest request) {
        log.debug("Creating new category with slug: {}", request.slug());
        Category category = new Category();
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = CacheConfig.CATEGORY_CACHE, allEntries = true)
    public CategoryDto updateCategory(Long id, CategoryRequest request) {
        log.debug("Updating category with id: {}", id);
        Category category = findCategoryOrThrow(id);
        applyRequest(category, request);
        return toDto(categoryRepository.save(category));
    }

    @CacheEvict(value = CacheConfig.CATEGORY_CACHE, allEntries = true)
    public void deleteCategory(Long id) {
        log.debug("Deleting category with id: {}", id);
        if (!categoryRepository.existsById(id)) {
            throw new NotFoundException("Category", id);
        }
        categoryRepository.deleteById(id);
    }

    @CacheEvict(value = CacheConfig.CATEGORY_CACHE, allEntries = true)
    public void addProductToCategory(Long categoryId, Long productId) {
        log.debug("Adding product {} to category {}", productId, categoryId);
        Category category = findCategoryOrThrow(categoryId);
        Product product = findProductOrThrow(productId);
        product.getCategories().add(category);
        productRepository.save(product);
    }

    @CacheEvict(value = CacheConfig.CATEGORY_CACHE, allEntries = true)
    public void removeProductFromCategory(Long categoryId, Long productId) {
        log.debug("Removing product {} from category {}", productId, categoryId);
        Product product = findProductOrThrow(productId);
        product.getCategories().removeIf(category -> category.getId().equals(categoryId));
        productRepository.save(product);
    }

    private Category findCategoryOrThrow(Long id) {
        return categoryRepository.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    private void applyRequest(Category category, CategoryRequest request) {
        category.setName(request.name());
        category.setSlug(request.slug());
        category.setDescription(request.description());
        category.setSortOrder(request.sortOrder() != null ? request.sortOrder() : 0);

        Long parentId = request.parentId();
        if (parentId == null) {
            category.setParent(null);
            return;
        }
        if (parentId.equals(category.getId())) {
            throw new InvalidCategoryHierarchyException("A category cannot be its own parent");
        }
        if (category.getId() != null) {
            assertNotDescendant(category.getId(), parentId);
        }
        Category parent = findCategoryOrThrow(parentId);
        category.setParent(parent);
    }

    private void assertNotDescendant(Long categoryId, Long candidateParentId) {
        Long currentId = candidateParentId;
        while (currentId != null) {
            if (currentId.equals(categoryId)) {
                throw new InvalidCategoryHierarchyException("Cannot assign a descendant category as parent");
            }
            currentId = categoryRepository.findParentId(currentId).orElse(null);
        }
    }

    private CategoryTreeDto toTreeDto(Category category, Map<Long, List<Category>> childrenByParentId) {
        List<CategoryTreeDto> children = childrenByParentId.getOrDefault(category.getId(), List.of()).stream()
                .sorted(Comparator.comparingInt(Category::getSortOrder))
                .map(child -> toTreeDto(child, childrenByParentId))
                .collect(Collectors.toCollection(ArrayList::new));
        return new CategoryTreeDto(
                category.getId(), category.getName(), category.getSlug(), category.getSortOrder(), children);
    }

    private CategoryDto toDto(Category category) {
        return new CategoryDto(
                category.getId(),
                category.getName(),
                category.getSlug(),
                category.getDescription(),
                category.getParent() != null ? category.getParent().getId() : null,
                category.getSortOrder(),
                category.getCreatedAt(),
                category.getUpdatedAt());
    }

    private ProductSummaryDto toProductSummaryDto(Product product) {
        return new ProductSummaryDto(
                product.getId(), product.getName(), product.getSku(), product.getBasePrice(), product.isActive());
    }
}
