package com.example.scaffold.admin;

import java.math.BigDecimal;
import java.util.List;

import com.example.scaffold.product.LowStockItemDto;

public record DashboardStatsDto(
        RevenueStats revenue, OrderStats orders, CustomerStats customers, LowStockStats lowStock) {

    public record RevenueStats(BigDecimal today, BigDecimal thisMonth, BigDecimal total) {
    }

    public record OrderStats(long today, long thisMonth, long pending) {
    }

    public record CustomerStats(long total, long newToday, long newThisMonth) {
    }

    public record LowStockStats(long count, List<LowStockItemDto> items) {
    }
}
