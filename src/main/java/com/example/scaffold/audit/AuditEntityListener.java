package com.example.scaffold.audit;

import java.lang.reflect.Field;

import org.springframework.beans.factory.annotation.Configurable;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.stereotype.Component;

import jakarta.persistence.PostLoad;
import jakarta.persistence.PostPersist;
import jakarta.persistence.PostRemove;
import jakarta.persistence.PostUpdate;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreRemove;
import jakarta.persistence.PreUpdate;
import lombok.extern.slf4j.Slf4j;

/**
 * JPA Entity Listener for automatic audit logging of entity lifecycle events.
 * This listener can be applied to entities using @EntityListeners annotation.
 */
@Component
@Configurable
@Slf4j
public class AuditEntityListener implements ApplicationContextAware {
    
    private static ApplicationContext applicationContext;
    private static AuditService auditService;
    
    @Override
    public void setApplicationContext(ApplicationContext applicationContext) {
        AuditEntityListener.applicationContext = applicationContext;
    }
    
    private AuditService getAuditService() {
        if (auditService == null && applicationContext != null) {
            auditService = applicationContext.getBean(AuditService.class);
        }
        return auditService;
    }
    
    @PrePersist
    public void prePersist(Object entity) {
        logEntityEvent(entity, "PRE_CREATE");
    }
    
    @PostPersist
    public void postPersist(Object entity) {
        logEntityEvent(entity, "CREATE");
    }
    
    @PreUpdate
    public void preUpdate(Object entity) {
        logEntityEvent(entity, "PRE_UPDATE");
    }
    
    @PostUpdate
    public void postUpdate(Object entity) {
        logEntityEvent(entity, "UPDATE");
    }
    
    @PreRemove
    public void preRemove(Object entity) {
        logEntityEvent(entity, "PRE_DELETE");
    }
    
    @PostRemove
    public void postRemove(Object entity) {
        logEntityEvent(entity, "DELETE");
    }
    
    @PostLoad
    public void postLoad(Object entity) {
        // Optionally log read operations (can be very verbose)
        // Uncomment the following line if you want to audit read operations
        // logEntityEvent(entity, "READ");
    }
    
    private void logEntityEvent(Object entity, String eventType) {
        try {
            AuditService service = getAuditService();
            if (service == null) {
                log.debug("AuditService not available, skipping audit for {} {}", 
                         entity.getClass().getSimpleName(), eventType);
                return;
            }
            
            String entityType = entity.getClass().getSimpleName().toUpperCase();
            Long entityId = extractEntityId(entity);
            
            // Create basic details about the entity
            String details = String.format("Entity: %s, Event: %s", 
                                          entity.getClass().getSimpleName(), eventType);
            
            service.logEvent(entityType, entityId, eventType, details);
            
        } catch (Exception e) {
            log.error("Failed to log entity audit event for {} {}", 
                     entity.getClass().getSimpleName(), eventType, e);
        }
    }
    
    /**
     * Extract entity ID using reflection.
     * Looks for fields named 'id' or annotated with @Id.
     */
    private Long extractEntityId(Object entity) {
        try {
            Class<?> entityClass = entity.getClass();
            
            // First try to find a field named 'id'
            try {
                Field idField = entityClass.getDeclaredField("id");
                idField.setAccessible(true);
                Object idValue = idField.get(entity);
                return convertToLong(idValue);
            } catch (NoSuchFieldException e) {
                // Field 'id' not found, continue with other approaches
            }
            
            // Look for @Id annotated fields
            Field[] fields = entityClass.getDeclaredFields();
            for (Field field : fields) {
                if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                    field.setAccessible(true);
                    Object idValue = field.get(entity);
                    return convertToLong(idValue);
                }
            }
            
            // Also check superclass fields
            Class<?> superClass = entityClass.getSuperclass();
            while (superClass != null && superClass != Object.class) {
                Field[] superFields = superClass.getDeclaredFields();
                for (Field field : superFields) {
                    if (field.isAnnotationPresent(jakarta.persistence.Id.class)) {
                        field.setAccessible(true);
                        Object idValue = field.get(entity);
                        return convertToLong(idValue);
                    }
                }
                superClass = superClass.getSuperclass();
            }
            
        } catch (Exception e) {
            log.debug("Could not extract entity ID from {}: {}", 
                     entity.getClass().getSimpleName(), e.getMessage());
        }
        
        return null;
    }
    
    private Long convertToLong(Object value) {
        if (value == null) {
            return null;
        }
        
        if (value instanceof Long) {
            return (Long) value;
        } else if (value instanceof Number) {
            return ((Number) value).longValue();
        } else {
            try {
                return Long.parseLong(value.toString());
            } catch (NumberFormatException e) {
                log.debug("Could not convert ID value '{}' to Long", value);
                return null;
            }
        }
    }
}