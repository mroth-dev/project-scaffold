package com.example.scaffold.product;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.example.scaffold.category.CategoryRepository;
import com.example.scaffold.category.CategorySummaryDto;
import com.example.scaffold.config.CacheConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.storage.ImageStorageService;
import com.example.scaffold.storage.StoredImage;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
public class ProductService {

    private static final String IMAGE_KEY_PREFIX = "products";

    private final ProductRepository productRepository;
    private final CategoryRepository categoryRepository;
    private final ProductVariationRepository productVariationRepository;
    private final ImageStorageService imageStorageService;

    @Value("${app.inventory.low-stock-threshold:5}")
    private int lowStockThreshold;

    public ProductService(
            ProductRepository productRepository,
            CategoryRepository categoryRepository,
            ProductVariationRepository productVariationRepository,
            ImageStorageService imageStorageService) {
        this.productRepository = productRepository;
        this.categoryRepository = categoryRepository;
        this.productVariationRepository = productVariationRepository;
        this.imageStorageService = imageStorageService;
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

    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#productId")
    public ProductDto addImages(Long productId, List<MultipartFile> files) {
        log.debug("Uploading {} image(s) for product {}", files.size(), productId);
        Product product = findProductOrThrow(productId);
        int nextSortOrder = product.getImages().stream().mapToInt(ProductImage::getSortOrder).max().orElse(-1) + 1;
        for (MultipartFile file : files) {
            if (file.isEmpty()) {
                continue;
            }
            ProductImage image = new ProductImage();
            image.setProduct(product);
            image.setImageKey(imageStorageService.store(IMAGE_KEY_PREFIX, file));
            image.setSortOrder(nextSortOrder++);
            product.getImages().add(image);
        }
        return toDto(productRepository.save(product));
    }

    @CacheEvict(value = CacheConfig.PRODUCT_CACHE, key = "#productId")
    public void deleteImage(Long productId, Long imageId) {
        log.debug("Deleting image {} for product {}", imageId, productId);
        Product product = findProductOrThrow(productId);
        ProductImage image = product.getImages().stream()
                .filter(candidate -> candidate.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("ProductImage", imageId));
        product.getImages().remove(image);
        productRepository.save(product);
        if (image.getImageKey() != null) {
            imageStorageService.delete(image.getImageKey());
        }
    }

    public StoredImage getImage(Long productId, Long imageId) {
        Product product = findProductOrThrow(productId);
        ProductImage image = product.getImages().stream()
                .filter(candidate -> candidate.getId().equals(imageId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("ProductImage", imageId));
        if (image.getImageKey() == null) {
            throw new NotFoundException("Image for product", productId);
        }
        return imageStorageService.retrieve(image.getImageKey());
    }

    public long countLowStock() {
        return productVariationRepository.countByInventoryCountLessThanEqual(lowStockThreshold);
    }

    public List<LowStockItemDto> getLowStockPreview(int limit) {
        return productVariationRepository
                .findByInventoryCountLessThanEqualOrderByInventoryCountAsc(lowStockThreshold, PageRequest.of(0, limit))
                .map(this::toLowStockDto)
                .getContent();
    }

    public Page<LowStockItemDto> getLowStockItems(Pageable pageable) {
        return productVariationRepository
                .findByInventoryCountLessThanEqualOrderByInventoryCountAsc(lowStockThreshold, pageable)
                .map(this::toLowStockDto);
    }

    private LowStockItemDto toLowStockDto(ProductVariation variation) {
        Product product = variation.getProduct();
        return new LowStockItemDto(
                product.getId(),
                product.getName(),
                variation.getId(),
                variation.getSku(),
                variation.getSize(),
                variation.getColor(),
                variation.getInventoryCount());
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
        if (imageRequests == null) {
            return;
        }
        product.getImages().clear();
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
                        imageUrl(product.getId(), image),
                        image.getImageKey() != null ? imageUrl(product.getId(), image) : image.getThumbnailUrl(),
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

    private static String imageUrl(Long productId, ProductImage image) {
        return image.getImageKey() != null
                ? "/products/" + productId + "/images/" + image.getId()
                : image.getUrl();
    }
}
