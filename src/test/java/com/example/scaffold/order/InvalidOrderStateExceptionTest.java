package com.example.scaffold.order;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.scaffold.exception.ConflictException;

class InvalidOrderStateExceptionTest {

    @Test
    void mapsTo409AndDescribesCurrentState() {
        InvalidOrderStateException ex = new InvalidOrderStateException(5L, OrderStatus.DELIVERED);

        assertEquals(HttpStatus.CONFLICT, ex.getStatus());
        assertTrue(ex.getMessage().contains("Order 5"));
        assertTrue(ex.getMessage().contains("DELIVERED"));
        assertTrue(ex instanceof ConflictException);
    }
}
