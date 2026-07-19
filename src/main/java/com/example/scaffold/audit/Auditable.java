package com.example.scaffold.audit;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation to mark methods that should be audited.
 * When applied to a method, it will automatically log audit events
 * before and/or after method execution.
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Auditable {
    
    /**
     * The entity type being audited (e.g., "USER", "PRODUCT", "ORDER")
     */
    String entityType();
    
    /**
     * The event type for this operation (e.g., "CREATE", "UPDATE", "DELETE", "VIEW")
     */
    String eventType();
    
    /**
     * SpEL expression to extract entity ID from method parameters or return value
     * Examples:
     * - "#result.id" - get ID from return value
     * - "#user.id" - get ID from parameter named 'user'
     * - "#p0.id" - get ID from first parameter
     * - "#id" - use parameter named 'id' directly
     */
    String entityIdExpression() default "";
    
    /**
     * SpEL expression to extract additional details for the audit log
     * Examples:
     * - "#user.email" - log user email
     * - "{'oldValue': #oldValue, 'newValue': #result}" - log before/after values
     */
    String detailsExpression() default "";
    
    /**
     * Whether to log before method execution (useful for DELETE operations)
     */
    boolean logBefore() default false;
    
    /**
     * Whether to log after method execution (default, useful for CREATE/UPDATE operations)
     */
    boolean logAfter() default true;
    
    /**
     * Whether to include method parameters in the audit details
     */
    boolean includeParameters() default false;
    
    /**
     * Whether to include return value in the audit details
     */
    boolean includeReturnValue() default false;
}