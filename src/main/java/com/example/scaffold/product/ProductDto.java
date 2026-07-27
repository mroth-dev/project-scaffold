package com.example.scaffold.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

import com.example.scaffold.category.CategorySummaryDto;

public record ProductDto(
        Long id,
        String name,
        String description,
        String sku,
        BigDecimal rrp,
        BigDecimal basePrice,
        boolean active,
        List<ProductImageDto> images,
        List<ProductVariationDto> variations,
        List<CategorySummaryDto> categories,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
