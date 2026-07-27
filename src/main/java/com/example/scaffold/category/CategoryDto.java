package com.example.scaffold.category;

import java.time.LocalDateTime;

public record CategoryDto(
        Long id,
        String name,
        String slug,
        String description,
        Long parentId,
        int sortOrder,
        String thumbnailImageUrl,
        String panelImageUrl,
        LocalDateTime createdAt,
        LocalDateTime updatedAt) {
}
