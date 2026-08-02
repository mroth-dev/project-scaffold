package com.example.scaffold.product;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.example.scaffold.storage.StoredImage;

/**
 * Streams uploaded product images back to browsers. Object storage
 * credentials and the bucket itself stay private to the app; nothing talks
 * to Garage directly.
 */
@RestController
@RequestMapping("/products")
public class ProductImageController {

    private final ProductService productService;

    public ProductImageController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping("/{productId}/images/{imageId}")
    public ResponseEntity<byte[]> image(@PathVariable Long productId, @PathVariable Long imageId) {
        StoredImage image = productService.getImage(productId, imageId);
        MediaType mediaType = image.contentType() != null
                ? MediaType.parseMediaType(image.contentType())
                : MediaType.APPLICATION_OCTET_STREAM;
        return ResponseEntity.ok()
                .contentType(mediaType)
                .header(HttpHeaders.CACHE_CONTROL, "public, max-age=3600")
                .body(image.content());
    }
}
