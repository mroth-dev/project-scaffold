package com.example.scaffold.product;

import java.math.BigDecimal;

public record ProductSummaryDto(
        Long id,
        String name,
        String sku,
        BigDecimal basePrice,
        boolean active) {
}
