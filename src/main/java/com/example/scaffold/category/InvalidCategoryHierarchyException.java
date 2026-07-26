package com.example.scaffold.category;

import com.example.scaffold.exception.ValidationException;

public class InvalidCategoryHierarchyException extends ValidationException {

    public InvalidCategoryHierarchyException(String message) {
        super(message);
    }
}
