package com.example.scaffold.category;

import jakarta.validation.constraints.NotBlank;

public record CategoryRequest(
        @NotBlank String name,
        @NotBlank String slug,
        String description,
        Long parentId,
        Integer sortOrder) {
}
