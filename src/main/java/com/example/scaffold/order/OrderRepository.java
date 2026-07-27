package com.example.scaffold.order;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface OrderRepository extends JpaRepository<Order, Long> {

    Page<Order> findByCustomerId(Long customerId, Pageable pageable);

    Page<Order> findByStatus(OrderStatus status, Pageable pageable);

    long countByStatus(OrderStatus status);

    long countByCreatedAtGreaterThanEqual(LocalDateTime since);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status <> :excludedStatus")
    BigDecimal sumRevenueExcludingStatus(OrderStatus excludedStatus);

    @Query("SELECT SUM(o.totalAmount) FROM Order o WHERE o.status <> :excludedStatus AND o.createdAt >= :since")
    BigDecimal sumRevenueExcludingStatusSince(OrderStatus excludedStatus, LocalDateTime since);
}
