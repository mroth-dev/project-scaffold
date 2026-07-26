package com.example.scaffold.user;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.scaffold.exception.ConflictException;

class DuplicateEmailExceptionTest {

    @Test
    void mapsTo409AndIncludesEmail() {
        DuplicateEmailException ex = new DuplicateEmailException("taken@example.com");

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("taken@example.com"));
        assertTrue(ex instanceof ConflictException);
    }
}
