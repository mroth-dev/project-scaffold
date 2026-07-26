package com.example.scaffold.order;

import java.math.BigDecimal;

public record OrderItemDto(
        Long id,
        Long productVariationId,
        String productName,
        String variationSku,
        int quantity,
        BigDecimal unitPrice,
        BigDecimal subtotal) {
}
