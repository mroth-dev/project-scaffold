package com.example.scaffold.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductVariationRequest(
        String size,
        String color,
        @NotBlank String sku,
        @PositiveOrZero Integer inventoryCount,
        BigDecimal priceAdjustment) {
}
