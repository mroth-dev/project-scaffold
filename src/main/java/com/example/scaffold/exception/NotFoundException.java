package com.example.scaffold.exception;

import org.springframework.http.HttpStatus;

public class NotFoundException extends BusinessException {

    public NotFoundException(String model, long id) {
        super(HttpStatus.NOT_FOUND, model + " not found with id: " + id);
    }

    public NotFoundException(String message) {
        super(HttpStatus.NOT_FOUND, message);
    }
}
