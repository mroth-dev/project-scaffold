package com.example.scaffold.audit;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;
import tools.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository auditEventRepository;

    private ObjectMapper objectMapper;
    private AuditService auditService;
    
    @BeforeEach
    void setUp() {
        objectMapper = new ObjectMapper();
        auditService = new AuditService(auditEventRepository, objectMapper);
        
        // Clear security context
        SecurityContextHolder.clearContext();
        RequestContextHolder.resetRequestAttributes();
    }

    @Test
    void logEvent_ShouldCreateAuditEventWithBasicInfo() {
        // Arrange
        String entityType = "USER";
        Long entityId = 123L;
        String eventType = "CREATE";
        String details = "User created";

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent(entityType, entityId, eventType, details);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals(entityType, savedEvent.getEntityType());
        assertEquals(entityId, savedEvent.getEntityId());
        assertEquals(eventType, savedEvent.getEventType());
        assertEquals(details, savedEvent.getDetails());
        assertNotNull(savedEvent.getTimestamp());
        assertTrue(savedEvent.getTimestamp().isBefore(LocalDateTime.now().plusSeconds(1)));
    }

    @Test
    void logEvent_WithAuthenticatedUser_ShouldIncludeUserId() {
        // Arrange
        CustomUserPrincipal userPrincipal = new CustomUserPrincipal(
            456L, "test@example.com", "password", List.of(), true);
        
        Authentication auth = new UsernamePasswordAuthenticationToken(userPrincipal, null, userPrincipal.getAuthorities());
        SecurityContextHolder.getContext().setAuthentication(auth);

        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent("PRODUCT", 789L, "UPDATE", "Product updated");

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals(456L, savedEvent.getUserId());
    }

    @Test
    void logEvent_WithHttpRequest_ShouldIncludeRequestInfo() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("192.168.1.100");
        request.addHeader("User-Agent", "Mozilla/5.0 Test Browser");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent("ORDER", 101L, "DELETE", "Order cancelled");

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals("192.168.1.100", savedEvent.getIpAddress());
        assertEquals("Mozilla/5.0 Test Browser", savedEvent.getUserAgent());
    }

    @Test
    void logEvent_WithXForwardedForHeader_ShouldUseForwardedIp() {
        // Arrange
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("10.0.0.1");  // Load balancer IP
        request.addHeader("X-Forwarded-For", "203.0.113.195, 70.41.3.18, 150.172.238.178");
        
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent("CATEGORY", 202L, "VIEW", "Category viewed");

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals("203.0.113.195", savedEvent.getIpAddress()); // First IP from X-Forwarded-For
    }

    @Test
    void logEvent_WithObjectDetails_ShouldSerializeToJson() {
        // Arrange
        Map<String, Object> detailsMap = new HashMap<>();
        detailsMap.put("field1", "value1");
        detailsMap.put("field2", 42);
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent("PRODUCT", 303L, "CREATE", detailsMap);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertTrue(savedEvent.getDetails().contains("field1"));
        assertTrue(savedEvent.getDetails().contains("value1"));
        assertTrue(savedEvent.getDetails().contains("field2"));
        assertTrue(savedEvent.getDetails().contains("42"));
    }

    @Test
    void logEvent_WithExplicitUserId_ShouldUseProvidedUserId() {
        // Arrange
        Long explicitUserId = 999L;
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent("USER", 404L, "LOGIN", "User logged in", explicitUserId);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals(explicitUserId, savedEvent.getUserId());
    }

    @Test
    void logAuthEvent_ShouldCreateAuthenticationAuditEvent() {
        // Arrange
        String eventType = "LOGIN_ATTEMPT";
        String username = "testuser@example.com";
        String details = "Failed login attempt";
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logAuthEvent(eventType, username, details);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals("USER", savedEvent.getEntityType());
        assertEquals(eventType, savedEvent.getEventType());
        assertTrue(savedEvent.getDetails().contains(username));
        assertTrue(savedEvent.getDetails().contains(details));
        assertNull(savedEvent.getUserId()); // No authenticated user for auth events
    }

    @Test
    void logEvent_SystemEvent_ShouldCreateSystemAuditEvent() {
        // Arrange
        String eventType = "SYSTEM_STARTUP";
        String details = "Application started successfully";
        
        when(auditEventRepository.save(any(AuditEvent.class))).thenReturn(new AuditEvent());

        // Act
        auditService.logEvent(eventType, details);

        // Assert
        ArgumentCaptor<AuditEvent> captor = ArgumentCaptor.forClass(AuditEvent.class);
        verify(auditEventRepository).save(captor.capture());

        AuditEvent savedEvent = captor.getValue();
        assertEquals("SYSTEM", savedEvent.getEntityType());
        assertNull(savedEvent.getEntityId());
        assertEquals(eventType, savedEvent.getEventType());
        assertEquals(details, savedEvent.getDetails());
    }

    @Test
    void logEvent_WhenRepositoryFails_ShouldNotThrowException() {
        // Arrange
        when(auditEventRepository.save(any(AuditEvent.class)))
                .thenThrow(new RuntimeException("Database error"));

        // Act & Assert - should not throw exception
        assertDoesNotThrow(() -> {
            auditService.logEvent("USER", 1L, "CREATE", "Test event");
        });
    }
}