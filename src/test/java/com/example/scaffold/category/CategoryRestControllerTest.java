package com.example.scaffold.category;

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

import java.time.LocalDateTime;
import java.util.List;

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
import com.example.scaffold.security.CustomUserDetailsService;
import com.example.scaffold.security.JwtTokenProvider;

import tools.jackson.databind.ObjectMapper;

@WebMvcTest(CategoryRestController.class)
@Import({SecurityConfig.class, JwtTokenProvider.class})
class CategoryRestControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private CategoryService categoryService;

    @MockitoBean
    private CustomUserDetailsService customUserDetailsService;

    // RateLimitingFilter sits in the security chain; rateLimitConfig.isEnabled()
    // defaults to false when mocked, so it's a no-op here.
    @MockitoBean
    private RateLimitService rateLimitService;

    @MockitoBean
    private RateLimitConfig rateLimitConfig;

    private CategoryDto sampleCategory() {
        return new CategoryDto(1L, "Clothing", "clothing", "Apparel", null, 0, LocalDateTime.now(), LocalDateTime.now());
    }

    @Test
    void getCategoryTreeReturnsList() throws Exception {
        given(categoryService.getCategoryTree()).willReturn(List.of(new CategoryTreeDto(1L, "Clothing", "clothing", 0, List.of())));

        mockMvc.perform(get("/api/categories/tree"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].slug").value("clothing"));
    }

    @Test
    void getCategoryReturnsSingleCategory() throws Exception {
        given(categoryService.getCategory(1L)).willReturn(sampleCategory());

        mockMvc.perform(get("/api/categories/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.slug").value("clothing"));
    }

    @Test
    void getCategoryReturns404WhenMissing() throws Exception {
        given(categoryService.getCategory(99L)).willThrow(new NotFoundException("Category", 99L));

        mockMvc.perform(get("/api/categories/99"))
                .andExpect(status().isNotFound());
    }

    @Test
    void createCategoryReturns201() throws Exception {
        CategoryRequest request = new CategoryRequest("Clothing", "clothing", "Apparel", null, 0);
        given(categoryService.createCategory(any(CategoryRequest.class))).willReturn(sampleCategory());

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void createCategoryReturns400WhenInvalid() throws Exception {
        CategoryRequest invalid = new CategoryRequest("", "", null, null, null);

        mockMvc.perform(post("/api/categories")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalid)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void deleteCategoryReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1"))
                .andExpect(status().isNoContent());

        verify(categoryService).deleteCategory(eq(1L));
    }

    @Test
    void addProductToCategoryReturns204() throws Exception {
        mockMvc.perform(post("/api/categories/1/products/5"))
                .andExpect(status().isNoContent());

        verify(categoryService).addProductToCategory(eq(1L), eq(5L));
    }

    @Test
    void removeProductFromCategoryReturns204() throws Exception {
        mockMvc.perform(delete("/api/categories/1/products/5"))
                .andExpect(status().isNoContent());

        verify(categoryService).removeProductFromCategory(eq(1L), eq(5L));
    }
}
