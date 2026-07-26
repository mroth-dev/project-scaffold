package com.example.scaffold.product;

public record ProductImageDto(
        Long id,
        String url,
        String thumbnailUrl,
        String altText,
        int sortOrder) {
}
