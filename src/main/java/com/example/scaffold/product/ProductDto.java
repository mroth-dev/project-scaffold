package com.example.scaffold.product;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

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
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
