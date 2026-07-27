package com.example.scaffold.category;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scaffold.storage.StoredImage;

/**
 * Streams category images back to browsers. Object storage credentials and
 * the bucket itself stay private to the app; nothing talks to Garage directly.
 */
@RestController
@RequestMapping("/categories")
public class CategoryImageController {

    private final CategoryService categoryService;

    public CategoryImageController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping("/{categoryId}/thumbnail")
    public ResponseEntity<byte[]> thumbnail(@PathVariable Long categoryId) {
        return imageResponse(categoryService.getThumbnail(categoryId));
    }

    @GetMapping("/{categoryId}/panel-image")
    public ResponseEntity<byte[]> panelImage(@PathVariable Long categoryId) {
        return imageResponse(categoryService.getPanelImage(categoryId));
    }

    private ResponseEntity<byte[]> imageResponse(StoredImage image) {
        MediaType mediaType = image.contentType() != null
                ? MediaType.parseMediaType(image.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(image.content());
    }
}
