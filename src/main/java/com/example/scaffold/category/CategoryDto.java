package com.example.scaffold.category;

import java.time.LocalDateTime;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        int sortOrder,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
