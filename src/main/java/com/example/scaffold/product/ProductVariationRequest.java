package com.example.scaffold.product;

import java.math.BigDecimal;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.PositiveOrZero;

public record ProductVariationRequest(
        String size,
        String color,
        @Pattern(regexp = "^#[0-9A-Fa-f]{6}$", message = "Color hex must look like #RRGGBB") String colorHex,
        @NotBlank String sku,
        @PositiveOrZero Integer inventoryCount,
        BigDecimal priceAdjustment) {
}
