package com.example.scaffold.order;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;

public record OrderRequest(
        @NotEmpty @Valid List<OrderItemRequest> items,
        String promotionCode) {

    public OrderRequest(@NotEmpty @Valid List<OrderItemRequest> items) {
        this(items, null);
    }
}
