package com.example.scaffold.audit;

import java.time.LocalDateTime;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long> {
    
    /**
     * Find audit events by entity type and ID
     */
    List<AuditEvent> findByEntityTypeAndEntityIdOrderByTimestampDesc(String entityType, Long entityId);
    
    /**
     * Find audit events by user ID
     */
    Page<AuditEvent> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    
    /**
     * Find audit events by event type
     */
    Page<AuditEvent> findByEventTypeOrderByTimestampDesc(String eventType, Pageable pageable);
    
    /**
     * Find audit events by entity type
     */
    Page<AuditEvent> findByEntityTypeOrderByTimestampDesc(String entityType, Pageable pageable);
    
    /**
     * Find audit events within a time range
     */
    Page<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);
    
    /**
     * Find audit events by multiple criteria with custom query
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE " +
           "(:entityType IS NULL OR ae.entityType = :entityType) AND " +
           "(:entityId IS NULL OR ae.entityId = :entityId) AND " +
           "(:eventType IS NULL OR ae.eventType = :eventType) AND " +
           "(:userId IS NULL OR ae.userId = :userId) AND " +
           "(:startTime IS NULL OR ae.timestamp >= :startTime) AND " +
           "(:endTime IS NULL OR ae.timestamp <= :endTime) " +
           "ORDER BY ae.timestamp DESC")
    Page<AuditEvent> findByFilters(
            @Param("entityType") String entityType,
            @Param("entityId") Long entityId,
            @Param("eventType") String eventType,
            @Param("userId") Long userId,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            Pageable pageable
    );
    
    /**
     * Count events by entity type
     */
    long countByEntityType(String entityType);
    
    /**
     * Count events by user ID
     */
    long countByUserId(Long userId);
    
    /**
     * Find recent audit events for a specific user
     */
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.userId = :userId " +
           "AND ae.timestamp >= :since ORDER BY ae.timestamp DESC")
    List<AuditEvent> findRecentEventsByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}