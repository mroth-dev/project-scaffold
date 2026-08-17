package com.example.scaffold.user;

public record AccountDto(
        Long id,
        String firstName,
        String lastName,
        String email,
        Address address) {
}
