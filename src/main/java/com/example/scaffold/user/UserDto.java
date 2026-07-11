package com.example.scaffold.user;

import java.time.LocalDate;

import com.example.scaffold.common.Gender;

public record UserDto(
        Long id,
        String name,
        String email,
        LocalDate birthDate,
        Gender gender) {
}
