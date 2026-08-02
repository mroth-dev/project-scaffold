package com.example.scaffold.promotion;

import java.math.BigDecimal;

/**
 * Result of evaluating a promotion code against a cart subtotal: what it
 * would take off the order, computed without mutating usage counts.
 */
public record PromotionQuote(
        Long promotionId, String code, String name, PromotionType type, BigDecimal discountAmount) {
}
