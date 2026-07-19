package com.example.scaffold.audit;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.example.scaffold.config.RateLimit;

import lombok.extern.slf4j.Slf4j;

@RestController
@RequestMapping("/api/audit")
@Slf4j
public class AuditRestController {
    
    private final AuditEventRepository auditEventRepository;
    
    @Autowired
    public AuditRestController(AuditEventRepository auditEventRepository) {
        this.auditEventRepository = auditEventRepository;
    }
    
    /**
     * Get audit events with filtering and pagination
     */
    @GetMapping
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @RateLimit(requests = 50, window = 60)
    public ResponseEntity<Page<AuditEvent>> getAuditEvents(
            @RequestParam(required = false) String entityType,
            @RequestParam(required = false) Long entityId,
            @RequestParam(required = false) String eventType,
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String startTime,
            @RequestParam(required = false) String endTime,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(defaultValue = "timestamp") String sortBy,
            @RequestParam(defaultValue = "desc") String sortDir) {
        
        try {
            // Validate and limit page size
            size = Math.min(Math.max(size, 1), 100); // Between 1 and 100
            page = Math.max(page, 0); // Non-negative
            
            // Create pageable with sorting
            Pageable pageable = PageRequest.of(page, size);
            if ("asc".equalsIgnoreCase(sortDir)) {
                // For ascending, we need to reverse the default DESC ordering
                // This is handled in the repository query
            }
            
            // Parse date strings
            LocalDateTime startDateTime = parseDateTime(startTime);
            LocalDateTime endDateTime = parseDateTime(endTime);
            
            Page<AuditEvent> auditEvents = auditEventRepository.findByFilters(
                    entityType, entityId, eventType, userId, 
                    startDateTime, endDateTime, pageable);
            
            return ResponseEntity.ok(auditEvents);
            
        } catch (Exception e) {
            log.error("Error retrieving audit events", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get audit events for a specific entity
     */
    @GetMapping("/entity/{entityType}/{entityId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @RateLimit(requests = 100, window = 60)
    public ResponseEntity<Page<AuditEvent>> getEntityAuditHistory(
            @PathVariable String entityType,
            @PathVariable Long entityId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            size = Math.min(Math.max(size, 1), 50); // Smaller limit for entity history
            page = Math.max(page, 0);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditEvent> auditEvents = auditEventRepository.findByFilters(
                    entityType, entityId, null, null, null, null, pageable);
            
            return ResponseEntity.ok(auditEvents);
            
        } catch (Exception e) {
            log.error("Error retrieving entity audit history for {}:{}", entityType, entityId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get audit events for a specific user's actions
     */
    @GetMapping("/user/{userId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or (hasRole('USER') and #userId == authentication.principal.id)")
    @RateLimit(requests = 100, window = 60)
    public ResponseEntity<Page<AuditEvent>> getUserAuditHistory(
            @PathVariable Long userId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        
        try {
            size = Math.min(Math.max(size, 1), 50);
            page = Math.max(page, 0);
            
            Pageable pageable = PageRequest.of(page, size);
            Page<AuditEvent> auditEvents = auditEventRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
            
            return ResponseEntity.ok(auditEvents);
            
        } catch (Exception e) {
            log.error("Error retrieving user audit history for user {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get recent audit events for a user (last 24 hours)
     */
    @GetMapping("/user/{userId}/recent")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER') or (hasRole('USER') and #userId == authentication.principal.id)")
    @RateLimit(requests = 200, window = 60)
    public ResponseEntity<?> getRecentUserActivity(@PathVariable Long userId) {
        
        try {
            LocalDateTime since = LocalDateTime.now().minusDays(1);
            var recentEvents = auditEventRepository.findRecentEventsByUser(userId, since);
            
            return ResponseEntity.ok(recentEvents);
            
        } catch (Exception e) {
            log.error("Error retrieving recent user activity for user {}", userId, e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Get audit statistics
     */
    @GetMapping("/stats")
    @PreAuthorize("hasRole('ADMIN') or hasRole('MANAGER')")
    @RateLimit(requests = 10, window = 60)
    public ResponseEntity<?> getAuditStatistics() {
        
        try {
            // Get some basic statistics
            long totalEvents = auditEventRepository.count();
            long userEvents = auditEventRepository.countByEntityType("USER");
            long productEvents = auditEventRepository.countByEntityType("PRODUCT");
            long orderEvents = auditEventRepository.countByEntityType("ORDER");
            
            var stats = new AuditStatistics(totalEvents, userEvents, productEvents, orderEvents);
            return ResponseEntity.ok(stats);
            
        } catch (Exception e) {
            log.error("Error retrieving audit statistics", e);
            return ResponseEntity.badRequest().build();
        }
    }
    
    /**
     * Parse date time string with flexible format support
     */
    private LocalDateTime parseDateTime(String dateTimeString) {
        if (dateTimeString == null || dateTimeString.trim().isEmpty()) {
            return null;
        }
        
        try {
            // Try ISO format first
            return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ISO_LOCAL_DATE_TIME);
        } catch (DateTimeParseException e) {
            try {
                // Try with just date (assume start of day)
                return LocalDateTime.parse(dateTimeString + "T00:00:00");
            } catch (DateTimeParseException e2) {
                try {
                    // Try common format
                    return LocalDateTime.parse(dateTimeString, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
                } catch (DateTimeParseException e3) {
                    log.warn("Could not parse date time: {}", dateTimeString);
                    throw new IllegalArgumentException("Invalid date format: " + dateTimeString);
                }
            }
        }
    }
    
    /**
     * DTO for audit statistics
     */
    public record AuditStatistics(
            long totalEvents,
            long userEvents,
            long productEvents,
            long orderEvents
    ) {}
}