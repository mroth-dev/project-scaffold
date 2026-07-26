package com.example.scaffold.category;

import java.util.List;

public record CategoryTreeDto(
        Long id,
        String name,
        String slug,
        int sortOrder,
        List<CategoryTreeDto> children) {
}
