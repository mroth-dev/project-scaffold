package com.example.scaffold.user;

import java.time.LocalDate;

import com.example.scaffold.common.Gender;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Past;

public record UserRequest(
        @NotBlank String name,
        @NotBlank @Email String email,
        @Past LocalDate birthDate,
        Gender gender) {
}
