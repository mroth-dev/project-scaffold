package com.example.scaffold.user;

import java.time.LocalDate;

import com.example.scaffold.common.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Size;

public record UserRequest(
        @NotBlank @Email String email,
        @NotBlank @Size(min = 8, max = 100) String password,
        String firstName,
        String lastName,
        @Past LocalDate birthDate,
        Gender gender,
        UserRole role) {
}
