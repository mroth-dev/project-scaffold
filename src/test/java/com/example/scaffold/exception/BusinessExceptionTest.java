package com.example.scaffold.exception;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class BusinessExceptionTest {

    @Test
    void notFoundExceptionUsesModelAndIdMessageWith404() {
        NotFoundException ex = new NotFoundException("Product", 42L);

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("Product not found with id: 42", ex.getMessage());
        assertInstanceOf(BusinessException.class, ex);
    }

    @Test
    void notFoundExceptionAcceptsCustomMessage() {
        NotFoundException ex = new NotFoundException("User with email: a@b.com not found");

        assertEquals(HttpStatus.NOT_FOUND, ex.getStatus());
        assertEquals("User with email: a@b.com not found", ex.getMessage());
    }

    @Test
    void validationExceptionMapsTo400() {
        ValidationException ex = new ValidationException("Field is invalid");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("Field is invalid", ex.getMessage());
        assertInstanceOf(BusinessException.class, ex);
    }

    @Test
    void conflictExceptionMapsTo409() {
        ConflictException ex = new ConflictException("Already exists");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertEquals("Already exists", ex.getMessage());
        assertInstanceOf(BusinessException.class, ex);
    }
}
