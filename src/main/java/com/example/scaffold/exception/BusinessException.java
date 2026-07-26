package com.example.scaffold.exception;

import org.springframework.http.HttpStatus;

/**
 * Base type for all business-rule violations raised by the service layer.
 * Carries the HTTP status the violation should map to, so a single
 * exception handler can respond consistently without per-type branching.
 */
public abstract class BusinessException extends RuntimeException {

    private final HttpStatus status;

    protected BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }
}
