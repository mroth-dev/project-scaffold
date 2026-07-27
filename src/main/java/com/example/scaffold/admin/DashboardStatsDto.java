package com.example.scaffold.admin;

import java.math.BigDecimal;

public record DashboardStatsDto(RevenueStats revenue, OrderStats orders, CustomerStats customers) {

    public record RevenueStats(BigDecimal today, BigDecimal thisMonth, BigDecimal total) {
    }

    public record OrderStats(long today, long thisMonth, long pending) {
    }

    public record CustomerStats(long total, long newToday, long newThisMonth) {
    }
}
