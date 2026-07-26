package com.example.scaffold.user;

import com.example.scaffold.exception.ConflictException;

public class DuplicateEmailException extends ConflictException {

    public DuplicateEmailException(String email) {
        super("An account with email " + email + " already exists");
    }
}
