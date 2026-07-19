package com.example.scaffold.user;

import java.time.LocalDate;
import java.time.LocalDateTime;

import com.example.scaffold.common.Gender;

public record UserDto(
        Long id,
        String email,
        String firstName,
        String lastName,
        LocalDate birthDate,
        Gender gender,
        UserRole role,
        AccountStatus status,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
