package com.example.scaffold.product;

import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.config.RateLimitConfig;
import com.example.scaffold.config.RateLimitService;
import com.example.scaffold.config.SecurityConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.security.CustomUserDetailsService;
import com.example.scaffold.security.JwtTokenProvider;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductRestController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
class ProductRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ProductService productService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // RateLimitingFilter sits in the security chain; rateLimitConfig.isEnabled()
    // defaults to false when mocked, so it's a no-op here.
    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    private ProductDto sampleProduct() {
        return new ProductDto(1L, "T-Shirt", "Cotton crew neck t-shirt", "TSHIRT-001",
                new BigDecimal("29.99"), new BigDecimal("19.99"), true,
                List.of(new ProductImageDto(
                        1L, "http://example.com/image.jpg", "http://example.com/thumb.jpg", "Front view", 0)),
                List.of(new ProductVariationDto(1L, "M", "Blue", "TSHIRT-001-M-BLUE", 10, BigDecimal.ZERO)),
                LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getProductsReturnsPage() throws Exception {
        given(productService.searchProducts(eq(null), any()))
                .willReturn(new PageImpl<>(List.of(sampleProduct()), PageRequest.of(0, 20), 1));

        mockMvc.perform(get("/api/products"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content", hasSize(1)))
                .andExpect(jsonPath("$.content[0].sku").value("TSHIRT-001"));
    }

    @Test
    void getProductReturnsSingleProduct() throws Exception {
        given(productService.getProduct(1L)).willReturn(sampleProduct());

        mockMvc.perform(get("/api/products/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.sku").value("TSHIRT-001"));
    }

    @Test
    void getProductReturns404WhenMissing() throws Exception {
        given(productService.getProduct(99L)).willThrow(new NotFoundException("Product", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createProductReturns201() throws Exception {
        ProductRequest request = new ProductRequest("T-Shirt", "Cotton crew neck t-shirt", "TSHIRT-001",
                new BigDecimal("29.99"), new BigDecimal("19.99"), true,
                List.of(), List.of());
        given(productService.createProduct(any(ProductRequest.class))).willReturn(sampleProduct());

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createProductReturns400WhenInvalid() throws Exception {
        ProductRequest invalid = new ProductRequest("", null, "", null, null, null, null, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteProductReturns204() throws Exception {
        mockMvc.perform(delete("/api/products/1"))
                .andExpect(status().isNoContent());

        verify(productService).deleteProduct(eq(1L));
    }
}
