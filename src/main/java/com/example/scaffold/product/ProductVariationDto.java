package com.example.scaffold.product;

import java.math.BigDecimal;

public record ProductVariationDto(
        Long id,
        String size,
        String color,
        String sku,
        int inventoryCount,
        BigDecimal priceAdjustment) {
}
