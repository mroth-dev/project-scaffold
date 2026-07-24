package com.example.scaffold.product;

import java.math.BigDecimal;
import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductRequest(
        @NotBlank String name,
        String description,
        @NotBlank String sku,
        @PositiveOrZero BigDecimal rrp,
        @NotNull @PositiveOrZero BigDecimal basePrice,
        Boolean active,
        @Valid List<ProductImageRequest> images,
        @Valid List<ProductVariationRequest> variations) {
}
