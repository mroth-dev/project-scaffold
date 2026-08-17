package com.example.scaffold.user;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record ProfileUpdateRequest(
        @NotBlank String firstName,
        String lastName,
        @NotBlank @Email String email,
        String currentPassword,
        String newPassword) {
}
