package com.example.scaffold.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

public record PromotionDto(
        Long id,
        String name,
        String code,
        String description,
        PromotionType type,
        BigDecimal value,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer usageLimit,
        Integer usageLimitPerCustomer,
        BigDecimal minimumPurchaseAmount,
        boolean active,
        int usageCount,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
