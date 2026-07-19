package com.example.scaffold.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

@ExtendWith(MockitoExtension.class)
class AuditRestControllerTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private AuditRestController auditRestController;
    private List<AuditEvent> testAuditEvents;

    @BeforeEach
    void setUp() {
        auditRestController = new AuditRestController(auditEventRepository);
        
        // Create test audit events
        testAuditEvents = List.of(
                createAuditEvent(1L, "USER", 100L, "CREATE", 1L),
                createAuditEvent(2L, "PRODUCT", 200L, "UPDATE", 1L),
                createAuditEvent(3L, "ORDER", 300L, "DELETE", 2L)
        );
    }

    @Test
    void getAuditEvents_ShouldReturnPagedResults() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(testAuditEvents);
        when(auditEventRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getAuditEvents(
                null, null, null, null, null, null, 0, 20, "timestamp", "desc");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(3, response.getBody().getContent().size());
    }

    @Test
    void getAuditEvents_WithFilters_ShouldPassFiltersCorrectly() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(List.of(testAuditEvents.get(0)));
        when(auditEventRepository.findByFilters(eq("USER"), eq(100L), eq("CREATE"), eq(1L), 
                any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getAuditEvents(
                "USER", 100L, "CREATE", 1L, null, null, 0, 20, "timestamp", "desc");

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(auditEventRepository).findByFilters(eq("USER"), eq(100L), eq("CREATE"), eq(1L), 
                any(), any(), any(Pageable.class));
    }

    @Test
    void getEntityAuditHistory_ShouldReturnEntitySpecificEvents() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(List.of(testAuditEvents.get(0)));
        when(auditEventRepository.findByFilters(eq("USER"), eq(100L), any(), any(), any(), any(), 
                any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getEntityAuditHistory(
                "USER", 100L, 0, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().getContent().size());
        assertEquals("USER", response.getBody().getContent().get(0).getEntityType());
        assertEquals(100L, response.getBody().getContent().get(0).getEntityId());
    }

    @Test
    void getUserAuditHistory_ShouldReturnUserSpecificEvents() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(List.of(testAuditEvents.get(0), testAuditEvents.get(1)));
        when(auditEventRepository.findByUserIdOrderByTimestampDesc(eq(1L), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getUserAuditHistory(1L, 0, 20);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(2, response.getBody().getContent().size());
        assertTrue(response.getBody().getContent().stream().allMatch(event -> event.getUserId().equals(1L)));
    }

    @Test
    void getRecentUserActivity_ShouldReturnRecentEvents() {
        // Arrange
        List<AuditEvent> recentEvents = List.of(testAuditEvents.get(0));
        when(auditEventRepository.findRecentEventsByUser(eq(1L), any(LocalDateTime.class)))
                .thenReturn(recentEvents);

        // Act
        ResponseEntity<?> response = auditRestController.getRecentUserActivity(1L);

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof List);
        @SuppressWarnings("unchecked")
        List<AuditEvent> events = (List<AuditEvent>) response.getBody();
        assertEquals(1, events.size());
        assertEquals(1L, events.get(0).getUserId());
    }

    @Test
    void getAuditStatistics_ShouldReturnCorrectStatistics() {
        // Arrange
        when(auditEventRepository.count()).thenReturn(1000L);
        when(auditEventRepository.countByEntityType("USER")).thenReturn(300L);
        when(auditEventRepository.countByEntityType("PRODUCT")).thenReturn(400L);
        when(auditEventRepository.countByEntityType("ORDER")).thenReturn(300L);

        // Act
        ResponseEntity<?> response = auditRestController.getAuditStatistics();

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertTrue(response.getBody() instanceof AuditRestController.AuditStatistics);
        
        AuditRestController.AuditStatistics stats = (AuditRestController.AuditStatistics) response.getBody();
        assertEquals(1000L, stats.totalEvents());
        assertEquals(300L, stats.userEvents());
        assertEquals(400L, stats.productEvents());
        assertEquals(300L, stats.orderEvents());
    }

    @Test
    void getAuditEvents_WithInvalidPageSize_ShouldLimitPageSize() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(testAuditEvents);
        when(auditEventRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getAuditEvents(
                null, null, null, null, null, null, 0, 200, "timestamp", "desc"); // Request more than max

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify that the page size was limited to maximum allowed (100)
        verify(auditEventRepository).findByFilters(any(), any(), any(), any(), any(), any(), 
                argThat(pageable -> pageable.getPageSize() <= 100));
    }

    @Test
    void getAuditEvents_WithNegativePage_ShouldUseDefaultPage() {
        // Arrange
        Page<AuditEvent> page = new PageImpl<>(testAuditEvents);
        when(auditEventRepository.findByFilters(any(), any(), any(), any(), any(), any(), any(Pageable.class)))
                .thenReturn(page);

        // Act
        ResponseEntity<Page<AuditEvent>> response = auditRestController.getAuditEvents(
                null, null, null, null, null, null, -1, 20, "timestamp", "desc"); // Negative page

        // Assert
        assertNotNull(response);
        assertEquals(HttpStatus.OK, response.getStatusCode());
        
        // Verify that the page number was corrected to 0
        verify(auditEventRepository).findByFilters(any(), any(), any(), any(), any(), any(), 
                argThat(pageable -> pageable.getPageNumber() >= 0));
    }

    private AuditEvent createAuditEvent(Long id, String entityType, Long entityId, String eventType, Long userId) {
        return AuditEvent.builder()
                .id(id)
                .entityType(entityType)
                .entityId(entityId)
                .eventType(eventType)
                .userId(userId)
                .details("Test event details")
                .timestamp(LocalDateTime.now())
                .ipAddress("127.0.0.1")
                .userAgent("Test User Agent")
                .build();
    }
}