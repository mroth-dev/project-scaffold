package com.example.scaffold.product;

import java.math.BigDecimal;
import java.util.List;

import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

    private final ProductRepository productRepository;

    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
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
        product.getVariations().clear();
        if (variationRequests == null) {
            return;
        }
        for (ProductVariationRequest variationRequest : variationRequests) {
            ProductVariation variation = new ProductVariation();
            variation.setProduct(product);
            variation.setSize(variationRequest.size());
            variation.setColor(variationRequest.color());
            variation.setSku(variationRequest.sku());
            variation.setInventoryCount(
                    variationRequest.inventoryCount() != null ? variationRequest.inventoryCount() : 0);
            variation.setPriceAdjustment(
                    variationRequest.priceAdjustment() != null ? variationRequest.priceAdjustment() : BigDecimal.ZERO);
            product.getVariations().add(variation);
        }
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
                product.getCreatedAt(),
                product.getUpdatedAt());
    }
}
