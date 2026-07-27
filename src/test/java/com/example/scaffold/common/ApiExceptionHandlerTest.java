package com.example.scaffold.common;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import com.example.scaffold.config.RateLimitConfig;
import com.example.scaffold.config.RateLimitService;
import com.example.scaffold.config.SecurityConfig;
import com.example.scaffold.exception.NotFoundException;
import com.example.scaffold.product.ProductRequest;
import com.example.scaffold.product.ProductRestController;
import com.example.scaffold.product.ProductService;
import com.example.scaffold.security.CustomUserDetailsService;
import com.example.scaffold.security.JwtTokenProvider;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(ProductRestController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
class ApiExceptionHandlerTest {

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

    @Test
    void notFoundReturnsConsistentApiErrorBody() throws Exception {
        given(productService.getProduct(99L)).willThrow(new NotFoundException("Product", 99L));

        mockMvc.perform(get("/api/products/99"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.error").value("Not Found"))
                .andExpect(jsonPath("$.message").value("Product not found with id: 99"))
                .andExpect(jsonPath("$.timestamp").exists());
    }

    @Test
    void validationFailureReturnsFieldLevelDetails() throws Exception {
        ProductRequest invalid = new ProductRequest("", null, "", null, null, null, null, null, null);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.details").isArray());
    }

    @Test
    void illegalArgumentFallsBackToBadRequestBody() throws Exception {
        given(productService.getProduct(5L)).willThrow(new IllegalArgumentException("Malformed request"));

        mockMvc.perform(get("/api/products/5"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request"));
    }

    @Test
    void unexpectedExceptionReturnsGenericMessageWithoutLeakingDetails() throws Exception {
        given(productService.getProduct(7L))
                .willThrow(new RuntimeException("password=hunter2 while querying products"));

        mockMvc.perform(get("/api/products/7"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred. Please try again later."));
    }
}
