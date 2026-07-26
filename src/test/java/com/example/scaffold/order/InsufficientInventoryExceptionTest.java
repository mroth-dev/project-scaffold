package com.example.scaffold.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.scaffold.exception.ConflictException;

class InsufficientInventoryExceptionTest {

    @Test
    void mapsTo409AndDescribesShortfall() {
        InsufficientInventoryException ex = new InsufficientInventoryException("TSHIRT-001-M-WHITE", 5, 2);

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("TSHIRT-001-M-WHITE"));
        assertTrue(ex.getMessage().contains("requested 5"));
        assertTrue(ex.getMessage().contains("available 2"));
        assertTrue(ex instanceof ConflictException);
    }
}
