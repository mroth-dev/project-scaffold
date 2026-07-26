package com.example.scaffold.product;

import jakarta.validation.constraints.NotBlank;

public record ProductImageRequest(
        @NotBlank String url,
        String thumbnailUrl,
        String altText,
        Integer sortOrder) {
}
