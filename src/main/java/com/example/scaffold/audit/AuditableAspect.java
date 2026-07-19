package com.example.scaffold.audit;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.AfterReturning;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.aspectj.lang.reflect.MethodSignature;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.expression.MethodBasedEvaluationContext;
import org.springframework.core.DefaultParameterNameDiscoverer;
import org.springframework.core.ParameterNameDiscoverer;
import org.springframework.expression.Expression;
import org.springframework.expression.spel.standard.SpelExpressionParser;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;

@Aspect
@Component
@Slf4j
public class AuditableAspect {
    
    private final AuditService auditService;
    private final SpelExpressionParser parser = new SpelExpressionParser();
    private final ParameterNameDiscoverer parameterNameDiscoverer = new DefaultParameterNameDiscoverer();
    
    @Autowired
    public AuditableAspect(AuditService auditService) {
        this.auditService = auditService;
    }
    
    @Before("@annotation(auditable)")
    public void auditBefore(JoinPoint joinPoint, Auditable auditable) {
        if (!auditable.logBefore()) {
            return;
        }
        
        try {
            processAuditEvent(joinPoint, auditable, null);
        } catch (Exception e) {
            log.error("Failed to process @Auditable before method execution", e);
        }
    }
    
    @AfterReturning(pointcut = "@annotation(auditable)", returning = "result")
    public void auditAfterReturning(JoinPoint joinPoint, Auditable auditable, Object result) {
        if (!auditable.logAfter()) {
            return;
        }
        
        try {
            processAuditEvent(joinPoint, auditable, result);
        } catch (Exception e) {
            log.error("Failed to process @Auditable after method execution", e);
        }
    }
    
    private void processAuditEvent(JoinPoint joinPoint, Auditable auditable, Object result) {
        MethodSignature signature = (MethodSignature) joinPoint.getSignature();
        Method method = signature.getMethod();
        Object[] args = joinPoint.getArgs();
        
        // Create evaluation context for SpEL expressions
        MethodBasedEvaluationContext context = new MethodBasedEvaluationContext(
            joinPoint.getTarget(), method, args, parameterNameDiscoverer);
        
        // Add result to context if available
        if (result != null) {
            context.setVariable("result", result);
        }
        
        // Extract entity ID
        Long entityId = extractEntityId(auditable.entityIdExpression(), context);
        
        // Extract details
        Object details = extractDetails(auditable, context, args, result);
        
        // Log the audit event
        auditService.logEvent(
            auditable.entityType(),
            entityId,
            auditable.eventType(),
            details
        );
    }
    
    private Long extractEntityId(String expression, MethodBasedEvaluationContext context) {
        if (expression == null || expression.trim().isEmpty()) {
            return null;
        }
        
        try {
            Expression exp = parser.parseExpression(expression);
            Object value = exp.getValue(context);
            
            if (value instanceof Long) {
                return (Long) value;
            } else if (value instanceof Number) {
                return ((Number) value).longValue();
            } else if (value != null) {
                return Long.parseLong(value.toString());
            }
        } catch (Exception e) {
            log.warn("Failed to extract entity ID using expression '{}': {}", expression, e.getMessage());
        }
        
        return null;
    }
    
    private Object extractDetails(Auditable auditable, MethodBasedEvaluationContext context, 
                                 Object[] args, Object result) {
        Map<String, Object> detailsMap = new HashMap<>();
        
        // Add custom details from expression
        if (auditable.detailsExpression() != null && !auditable.detailsExpression().trim().isEmpty()) {
            try {
                Expression exp = parser.parseExpression(auditable.detailsExpression());
                Object customDetails = exp.getValue(context);
                if (customDetails != null) {
                    detailsMap.put("custom", customDetails);
                }
            } catch (Exception e) {
                log.warn("Failed to extract details using expression '{}': {}", 
                        auditable.detailsExpression(), e.getMessage());
            }
        }
        
        // Add method parameters if requested
        if (auditable.includeParameters() && args != null && args.length > 0) {
            Map<String, Object> parameters = new HashMap<>();
            String[] paramNames = parameterNameDiscoverer.getParameterNames(
                ((MethodSignature) context.getRootObject()).getMethod());
            
            for (int i = 0; i < args.length && i < (paramNames != null ? paramNames.length : args.length); i++) {
                String paramName = paramNames != null ? paramNames[i] : "param" + i;
                parameters.put(paramName, sanitizeParameter(args[i]));
            }
            detailsMap.put("parameters", parameters);
        }
        
        // Add return value if requested
        if (auditable.includeReturnValue() && result != null) {
            detailsMap.put("returnValue", sanitizeParameter(result));
        }
        
        // Return single custom value if that's all we have, otherwise return the map
        if (detailsMap.size() == 1 && detailsMap.containsKey("custom")) {
            return detailsMap.get("custom");
        } else if (detailsMap.isEmpty()) {
            return null;
        } else {
            return detailsMap;
        }
    }
    
    /**
     * Sanitize parameter values to avoid logging sensitive information
     */
    private Object sanitizeParameter(Object param) {
        if (param == null) {
            return null;
        }
        
        // Don't log password fields or large objects
        String paramString = param.toString();
        if (paramString.toLowerCase().contains("password") || 
            paramString.toLowerCase().contains("secret") ||
            paramString.toLowerCase().contains("token")) {
            return "[REDACTED]";
        }
        
        // Limit string length to prevent log bloat
        if (paramString.length() > 500) {
            return paramString.substring(0, 500) + "... [TRUNCATED]";
        }
        
        return param;
    }
}