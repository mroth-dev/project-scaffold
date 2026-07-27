package com.example.scaffold.admin;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

import org.springframework.stereotype.Service;

import com.example.scaffold.order.OrderRepository;
import com.example.scaffold.order.OrderStatus;
import com.example.scaffold.user.UserRepository;
import com.example.scaffold.user.UserRole;

@Service
public class DashboardService {

    private final OrderRepository orderRepository;
    private final UserRepository userRepository;

    public DashboardService(OrderRepository orderRepository, UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
    }

    public DashboardStatsDto getStats() {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        LocalDateTime startOfMonth = LocalDate.now().withDayOfMonth(1).atStartOfDay();

        // Cancelled orders never represent realized revenue, so they're excluded here.
        DashboardStatsDto.RevenueStats revenue = new DashboardStatsDto.RevenueStats(
                orZero(orderRepository.sumRevenueExcludingStatusSince(OrderStatus.CANCELLED, startOfToday)),
                orZero(orderRepository.sumRevenueExcludingStatusSince(OrderStatus.CANCELLED, startOfMonth)),
                orZero(orderRepository.sumRevenueExcludingStatus(OrderStatus.CANCELLED)));

        DashboardStatsDto.OrderStats orders = new DashboardStatsDto.OrderStats(
                orderRepository.countByCreatedAtGreaterThanEqual(startOfToday),
                orderRepository.countByCreatedAtGreaterThanEqual(startOfMonth),
                orderRepository.countByStatus(OrderStatus.PENDING));

        DashboardStatsDto.CustomerStats customers = new DashboardStatsDto.CustomerStats(
                userRepository.countByRole(UserRole.CUSTOMER),
                userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.CUSTOMER, startOfToday),
                userRepository.countByRoleAndCreatedAtGreaterThanEqual(UserRole.CUSTOMER, startOfMonth));

        return new DashboardStatsDto(revenue, orders, customers);
    }

    private static BigDecimal orZero(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
