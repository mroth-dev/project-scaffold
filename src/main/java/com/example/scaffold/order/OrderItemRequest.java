package com.example.scaffold.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

public record OrderItemRequest(
        @NotNull Long productVariationId,
        @Positive int quantity) {
}
