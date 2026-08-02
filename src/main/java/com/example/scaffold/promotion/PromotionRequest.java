package com.example.scaffold.promotion;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record PromotionRequest(
        @NotBlank String name,
        @NotBlank String code,
        String description,
        @NotNull PromotionType type,
        BigDecimal value,
        LocalDateTime startDate,
        LocalDateTime endDate,
        Integer usageLimit,
        Integer usageLimitPerCustomer,
        BigDecimal minimumPurchaseAmount,
        boolean active) {
}
