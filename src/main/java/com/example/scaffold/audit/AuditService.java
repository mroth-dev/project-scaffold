package com.example.scaffold.audit;

import java.time.LocalDateTime;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import com.example.scaffold.security.CustomUserDetailsService.CustomUserPrincipal;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;
import lombok.extern.slf4j.Slf4j;

@Service
@Slf4j
public class AuditService {
    
    private final AuditEventRepository auditEventRepository;
    private final ObjectMapper objectMapper;
    
    @Autowired
    public AuditService(AuditEventRepository auditEventRepository, ObjectMapper objectMapper) {
        this.auditEventRepository = auditEventRepository;
        this.objectMapper = objectMapper;
    }
    
    /**
     * Log an audit event with current user context
     */
    public void logEvent(String entityType, Long entityId, String eventType, Object details) {
        try {
            Long userId = getCurrentUserId();
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            
            AuditEvent auditEvent = AuditEvent.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .eventType(eventType)
                    .userId(userId)
                    .details(convertDetailsToJson(details))
                    .timestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            auditEventRepository.save(auditEvent);
            log.debug("Audit event logged: {} {} for entity {}:{}", eventType, entityType, entityType, entityId);
            
        } catch (Exception e) {
            log.error("Failed to log audit event for {}:{} - {}", entityType, entityId, eventType, e);
        }
    }
    
    /**
     * Log an audit event with explicit user ID
     */
    public void logEvent(String entityType, Long entityId, String eventType, Object details, Long userId) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            
            AuditEvent auditEvent = AuditEvent.builder()
                    .entityType(entityType)
                    .entityId(entityId)
                    .eventType(eventType)
                    .userId(userId)
                    .details(convertDetailsToJson(details))
                    .timestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            auditEventRepository.save(auditEvent);
            log.debug("Audit event logged: {} {} for entity {}:{} by user {}", eventType, entityType, entityType, entityId, userId);
            
        } catch (Exception e) {
            log.error("Failed to log audit event for {}:{} - {} by user {}", entityType, entityId, eventType, userId, e);
        }
    }
    
    /**
     * Log a simple event without entity context
     */
    public void logEvent(String eventType, Object details) {
        logEvent("SYSTEM", null, eventType, details);
    }
    
    /**
     * Log authentication events
     */
    public void logAuthEvent(String eventType, String username, String details) {
        try {
            String ipAddress = getClientIpAddress();
            String userAgent = getUserAgent();
            
            AuditEvent auditEvent = AuditEvent.builder()
                    .entityType("USER")
                    .entityId(null) // Username might not have ID yet
                    .eventType(eventType)
                    .userId(null) // User might not be authenticated yet
                    .details(String.format("Username: %s, Details: %s", username, details))
                    .timestamp(LocalDateTime.now())
                    .ipAddress(ipAddress)
                    .userAgent(userAgent)
                    .build();
            
            auditEventRepository.save(auditEvent);
            log.debug("Auth event logged: {} for username {}", eventType, username);
            
        } catch (Exception e) {
            log.error("Failed to log auth event {} for username {}", eventType, username, e);
        }
    }
    
    /**
     * Get current authenticated user ID
     */
    private Long getCurrentUserId() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.isAuthenticated() && 
                !"anonymousUser".equals(authentication.getName())) {
                
                Object principal = authentication.getPrincipal();
                log.debug("Principal type: {}, Principal: {}", 
                         principal != null ? principal.getClass().getSimpleName() : "null", principal);
                
                if (principal instanceof CustomUserPrincipal) {
                    return ((CustomUserPrincipal) principal).getId();
                }
                // Handle other principal types if needed
            }
        } catch (Exception e) {
            log.debug("Could not get current user ID: {}", e.getMessage());
        }
        return null;
    }
    
    /**
     * Get client IP address from request
     */
    private String getClientIpAddress() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            
            // Check for X-Forwarded-For header (common in load balancers/proxies)
            String xForwardedFor = request.getHeader("X-Forwarded-For");
            if (xForwardedFor != null && !xForwardedFor.isEmpty()) {
                // X-Forwarded-For can contain multiple IPs, take the first one
                return xForwardedFor.split(",")[0].trim();
            }
            
            // Check for X-Real-IP header (common in nginx)
            String xRealIp = request.getHeader("X-Real-IP");
            if (xRealIp != null && !xRealIp.isEmpty()) {
                return xRealIp;
            }
            
            // Fallback to remote address
            return request.getRemoteAddr();
            
        } catch (Exception e) {
            log.debug("Could not get client IP address: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Get user agent from request
     */
    private String getUserAgent() {
        try {
            ServletRequestAttributes attrs = (ServletRequestAttributes) RequestContextHolder.currentRequestAttributes();
            HttpServletRequest request = attrs.getRequest();
            return request.getHeader("User-Agent");
        } catch (Exception e) {
            log.debug("Could not get user agent: {}", e.getMessage());
            return null;
        }
    }
    
    /**
     * Convert details object to JSON string
     */
    private String convertDetailsToJson(Object details) {
        if (details == null) {
            return null;
        }
        
        if (details instanceof String) {
            return (String) details;
        }
        
        try {
            return objectMapper.writeValueAsString(details);
        } catch (JacksonException e) {
            log.warn("Failed to convert details to JSON, using toString: {}", e.getMessage());
            return details.toString();
        }
    }
}