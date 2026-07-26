package com.example.scaffold.category;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

import com.example.scaffold.exception.ValidationException;

class InvalidCategoryHierarchyExceptionTest {

    @Test
    void mapsTo400WithGivenMessage() {
        InvalidCategoryHierarchyException ex = new InvalidCategoryHierarchyException("A category cannot be its own parent");

        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatus());
        assertEquals("A category cannot be its own parent", ex.getMessage());
        assertEquals(ValidationException.class, ex.getClass().getSuperclass());
    }
}
