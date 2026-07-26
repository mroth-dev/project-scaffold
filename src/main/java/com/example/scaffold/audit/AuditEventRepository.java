package com.example.scaffold.audit;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface AuditEventRepository extends JpaRepository<AuditEvent, Long>, JpaSpecificationExecutor<AuditEvent> {

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
    Page<AuditEvent> findByTimestampBetweenOrderByTimestampDesc(
            LocalDateTime start, LocalDateTime end, Pageable pageable);

    /**
     * Find audit events by multiple optional criteria.
     *
     * <p>Built as a {@link Specification} rather than a single JPQL "{@code :x IS
     * NULL OR ...}" query: Postgres/pgjdbc can't determine the bind type of a bare
     * null {@link LocalDateTime} parameter used only in an "IS NULL" branch (raises
     * "could not determine data type of parameter"), so absent filters need to be
     * left out of the query entirely instead of bound as null.
     */
    default Page<AuditEvent> findByFilters(String entityType, Long entityId, String eventType, Long userId,
            LocalDateTime startTime, LocalDateTime endTime, Pageable pageable) {
        List<Specification<AuditEvent>> predicates = new ArrayList<>();
        if (entityType != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("entityType"), entityType));
        }
        if (entityId != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("entityId"), entityId));
        }
        if (eventType != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("eventType"), eventType));
        }
        if (userId != null) {
            predicates.add((root, query, cb) -> cb.equal(root.get("userId"), userId));
        }
        if (startTime != null) {
            predicates.add((root, query, cb) -> cb.greaterThanOrEqualTo(root.get("timestamp"), startTime));
        }
        if (endTime != null) {
            predicates.add((root, query, cb) -> cb.lessThanOrEqualTo(root.get("timestamp"), endTime));
        }
        Specification<AuditEvent> spec = Specification.allOf(predicates);
        Pageable ordered = PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "timestamp"));
        return findAll(spec, ordered);
    }

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
    @Query("SELECT ae FROM AuditEvent ae WHERE ae.userId = :userId "
           + "AND ae.timestamp >= :since ORDER BY ae.timestamp DESC")
    List<AuditEvent> findRecentEventsByUser(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
