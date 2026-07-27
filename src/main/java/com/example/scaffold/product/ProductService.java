package com.example.scaffold.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.scaffold.category.CategoryRepository;
import com.example.scaffold.category.CategorySummaryDto;
import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;

    public ProductService(ProductRepository productRepository, CategoryRepository categoryRepository) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
    }

    public Page<ProductDto> searchProducts(String query, Pageable pageable) {
        log.debug("Searching products with query: {} (not cached)", query);
        Page<Product> products = (query == null || query.isBlank())
                ? productRepository.findAll(pageable)
                : productRepository.search(query, pageable);
        return products.map(this::toDto);
    }

    public Page<ProductDto> getProductsByCategory(Long categoryId, Pageable pageable) {
        log.debug("Fetching products in category: {} (not cached)", categoryId);
        return productRepository.findByCategoriesId(categoryId, pageable).map(this::toDto);
    }

    @Cacheable(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductDto getProduct(Long id) {
        log.debug("Fetching product by id: {}", id);
        return toDto(findProductOrThrow(id));
    }

    public ProductDto createProduct(ProductRequest request) {
        log.debug("Creating new product with sku: {}", request.sku());
        Product product = new Product();
        applyRequest(product, request);
        Product savedProduct = productRepository.save(product);
        return toDto(savedProduct);
    }

    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public ProductDto updateProduct(Long id, ProductRequest request) {
        log.debug("Updating product with id: {}", id);
        Product product = findProductOrThrow(id);
        applyRequest(product, request);
        Product savedProduct = productRepository.save(product);
        log.debug("Cache evicted for product update: {}", id);
        return toDto(savedProduct);
    }

    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#id")
    public void deleteProduct(Long id) {
        log.debug("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new NotFoundException("Product", id);
        }
        productRepository.deleteById(id);
        log.debug("Cache evicted for product deletion: {}", id);
    }

    private Product findProductOrThrow(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new NotFoundException("Product", id));
    }

    private void applyRequest(Product product, ProductRequest request) {
        product.setName(request.name());
        product.setDescription(request.description());
        product.setSku(request.sku());
        product.setRrp(request.rrp());
        product.setBasePrice(request.basePrice());
        if (request.active() != null) {
            product.setActive(request.active());
        }
        syncImages(product, request.images());
        syncVariations(product, request.variations());
        syncCategories(product, request.categoryIds());
    }

    private void syncImages(Product product, List<ProductImageRequest> imageRequests) {
        product.getImages().clear();
        if (imageRequests == null) {
            return;
        }
        for (ProductImageRequest imageRequest : imageRequests) {
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setUrl(imageRequest.url());
            image.setThumbnailUrl(imageRequest.thumbnailUrl());
            image.setAltText(imageRequest.altText());
            image.setSortOrder(imageRequest.sortOrder() != null ? imageRequest.sortOrder() : 0);
            product.getImages().add(image);
        }
    }

    private void syncVariations(Product product, List<ProductVariationRequest> variationRequests) {
        if (variationRequests == null) {
            product.getVariations().clear();
            return;
        }
        // Reuse existing rows matched by SKU instead of deleting and recreating
        // every variation: since `sku` is globally unique, a blind clear-then-add
        // schedules inserts of unchanged SKUs before Hibernate flushes the deletes
        // of the rows they're replacing, tripping the unique constraint.
        Map<String, ProductVariation> existingBySku = product.getVariations().stream()
                .collect(Collectors.toMap(ProductVariation::getSku, variation -> variation));
        List<ProductVariation> reconciled = new ArrayList<>();
        for (ProductVariationRequest variationRequest : variationRequests) {
            ProductVariation variation = existingBySku.remove(variationRequest.sku());
            if (variation == null) {
                variation = new ProductVariation();
                variation.setProduct(product);
                variation.setSku(variationRequest.sku());
            }
            variation.setSize(variationRequest.size());
            variation.setColor(variationRequest.color());
            variation.setInventoryCount(
                    variationRequest.inventoryCount() != null ? variationRequest.inventoryCount() : 0);
            variation.setPriceAdjustment(
                    variationRequest.priceAdjustment() != null ? variationRequest.priceAdjustment() : BigDecimal.ZERO);
            reconciled.add(variation);
        }
        product.getVariations().clear();
        product.getVariations().addAll(reconciled);
    }

    private void syncCategories(Product product, List<Long> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            product.getCategories().clear();
            return;
        }
        product.setCategories(new HashSet<>(categoryRepository.findAllById(categoryIds)));
    }

    private ProductDto toDto(Product product) {
        List<ProductImageDto> images = product.getImages().stream()
                .map(image -> new ProductImageDto(
                        image.getId(),
                        image.getUrl(),
                        image.getThumbnailUrl(),
                        image.getAltText(),
                        image.getSortOrder()))
                .toList();
        List<ProductVariationDto> variations = product.getVariations().stream()
                .map(variation -> new ProductVariationDto(
                        variation.getId(),
                        variation.getSize(),
                        variation.getColor(),
                        variation.getSku(),
                        variation.getInventoryCount(),
                        variation.getPriceAdjustment()))
                .toList();
        List<CategorySummaryDto> categories = product.getCategories().stream()
                .map(category -> new CategorySummaryDto(category.getId(), category.getName()))
                .sorted(Comparator.comparing(CategorySummaryDto::name))
                .toList();
        return new ProductDto(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getSku(),
                product.getRrp(),
                product.getBasePrice(),
                product.isActive(),
                images,
                variations,
                categories,
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
