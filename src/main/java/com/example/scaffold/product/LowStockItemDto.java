package com.example.scaffold.product;

public record LowStockItemDto(
        Long productId,
        String productName,
        Long variationId,
        String sku,
        String size,
        String color,
        int inventoryCount) {
}
