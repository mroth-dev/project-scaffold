package com.example.scaffold.config;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Annotation for configuring rate limiting on controller methods
 *
 * This annotation can be applied to controller methods to specify
 * custom rate limiting rules that override the default configuration.
 */
@Target({ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
public @interface RateLimit {

    /**
     * Maximum number of requests allowed within the time window
     */
    int requests() default -1; // -1 means use default from configuration

    /**
     * Time window in seconds
     */
    int window() default -1; // -1 means use default from configuration

    /**
     * Key pattern for rate limiting. Supports placeholders:
     * - {ip} for client IP address
     * - {user} for authenticated user ID
     * - {endpoint} for request endpoint
     *
     * Default uses IP + endpoint combination
     */
    String keyPattern() default "{ip}:{endpoint}";

    /**
     * Whether to use per-user rate limiting for authenticated requests
     * If true, authenticated users get separate rate limits based on user ID
     */
    boolean perUser() default false;

    /**
     * Error message to return when rate limit is exceeded
     */
    String message() default "Rate limit exceeded. Please try again later.";
}
