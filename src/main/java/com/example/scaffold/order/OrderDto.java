package com.example.scaffold.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

public record OrderDto(
        Long id,
        Long customerId,
        OrderStatus status,
        BigDecimal totalAmount,
        String promotionCode,
        BigDecimal discountAmount,
        List<OrderItemDto> items,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
