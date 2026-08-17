package com.example.scaffold.user;

import jakarta.validation.constraints.NotBlank;

public record AddressUpdateRequest(
        @NotBlank String line1,
        String line2,
        @NotBlank String city,
        String region,
        @NotBlank String postcode,
        @NotBlank String country,
        String phone) {
}
