package com.example.scaffold.exception;

public class NotFoundException extends RuntimeException {

    public NotFoundException(String model, long id) {
        super(model +" not found with id: " + id);
    }
}
