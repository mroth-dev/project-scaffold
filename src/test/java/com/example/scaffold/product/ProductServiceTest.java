package com.example.scaffold.product;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import com.example.scaffold.exception.NotFoundException;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    private ProductRepository productRepository;

    private ProductService productService;

    private Product testProduct;
    private ProductRequest testProductRequest;

    @BeforeEach
    void setUp() {
        productService = new ProductService(productRepository);

        testProduct = new Product();
        testProduct.setId(1L);
        testProduct.setName("T-Shirt");
        testProduct.setDescription("Cotton crew neck t-shirt");
        testProduct.setSku("TSHIRT-001");
        testProduct.setRrp(new BigDecimal("29.99"));
        testProduct.setBasePrice(new BigDecimal("19.99"));
        testProduct.setActive(true);
        testProduct.setCreatedAt(LocalDateTime.now());
        testProduct.setUpdatedAt(LocalDateTime.now());

        testProductRequest = new ProductRequest(
                "T-Shirt",
                "Cotton crew neck t-shirt",
                "TSHIRT-001",
                new BigDecimal("29.99"),
                new BigDecimal("19.99"),
                true,
                List.of(new ProductImageRequest(
                        "http://example.com/image.jpg", "http://example.com/thumb.jpg", "Front view", 0)),
                List.of(new ProductVariationRequest("M", "Blue", "TSHIRT-001-M-BLUE", 10, BigDecimal.ZERO)));
    }

    @Test
    void getProductReturnsMappedDto() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));

        ProductDto result = productService.getProduct(1L);

        assertNotNull(result);
        assertEquals(testProduct.getId(), result.id());
        assertEquals(testProduct.getSku(), result.sku());
        verify(productRepository, times(1)).findById(1L);
    }

    @Test
    void getProductThrowsWhenMissing() {
        when(productRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(NotFoundException.class, () -> productService.getProduct(99L));
        verify(productRepository, times(1)).findById(99L);
    }

    @Test
    void createProductPersistsImagesAndVariations() {
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductDto created = productService.createProduct(testProductRequest);

        assertNotNull(created);
        assertEquals("TSHIRT-001", created.sku());
        assertEquals(1, created.images().size());
        assertEquals("http://example.com/image.jpg", created.images().get(0).url());
        assertEquals(1, created.variations().size());
        assertEquals("TSHIRT-001-M-BLUE", created.variations().get(0).sku());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void updateProductReplacesExistingImagesAndVariations() {
        when(productRepository.findById(1L)).thenReturn(Optional.of(testProduct));
        when(productRepository.save(any(Product.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ProductRequest updateRequest = new ProductRequest(
                "T-Shirt v2",
                "Updated description",
                "TSHIRT-001",
                new BigDecimal("34.99"),
                new BigDecimal("24.99"),
                false,
                List.of(),
                List.of());

        ProductDto updated = productService.updateProduct(1L, updateRequest);

        assertEquals("T-Shirt v2", updated.name());
        assertFalse(updated.active());
        assertTrue(updated.images().isEmpty());
        assertTrue(updated.variations().isEmpty());
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void deleteProductRemovesExistingProduct() {
        when(productRepository.existsById(1L)).thenReturn(true);

        productService.deleteProduct(1L);

        verify(productRepository, times(1)).deleteById(1L);
    }

    @Test
    void deleteProductThrowsWhenMissing() {
        when(productRepository.existsById(99L)).thenReturn(false);

        assertThrows(NotFoundException.class, () -> productService.deleteProduct(99L));
        verify(productRepository, never()).deleteById(anyLong());
    }

    @Test
    void searchProductsWithBlankQueryUsesFindAll() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.findAll(pageable)).thenReturn(page);

        Page<ProductDto> result = productService.searchProducts(" ", pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).findAll(pageable);
        verify(productRepository, never()).search(anyString(), any());
    }

    @Test
    void searchProductsWithQueryUsesSearch() {
        Pageable pageable = PageRequest.of(0, 10);
        Page<Product> page = new PageImpl<>(List.of(testProduct), pageable, 1);
        when(productRepository.search("shirt", pageable)).thenReturn(page);

        Page<ProductDto> result = productService.searchProducts("shirt", pageable);

        assertEquals(1, result.getTotalElements());
        verify(productRepository, times(1)).search("shirt", pageable);
    }
}
