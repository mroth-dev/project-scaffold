package com.example.scaffold.storage;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import software.amazon.awssdk.core.ResponseInputStream;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.GetObjectResponse;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

/**
 * Stores and retrieves uploaded images in the S3-compatible object store.
 * Callers keep only the returned key; the store never exposes a browsable
 * URL directly, so image bytes always flow back through the app.
 */
@Service
public class ImageStorageService {

    private final S3Client s3Client;
    private final StorageProperties properties;

    public ImageStorageService(S3Client s3Client, StorageProperties properties) {
        this.s3Client = s3Client;
        this.properties = properties;
    }

    public String store(String keyPrefix, MultipartFile file) {
        String key = keyPrefix + "/" + UUID.randomUUID() + extensionOf(file.getOriginalFilename());
        try {
            // fromBytes signs the whole body in one shot; fromInputStream signs it as
            // AWS's chunked-with-trailing-checksum stream, which Garage (like several
            // other S3-compatible stores) rejects with "Invalid payload signature".
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(properties.getBucket())
                            .key(key)
                            .contentType(file.getContentType())
                            .build(),
                    RequestBody.fromBytes(file.getBytes()));
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read uploaded image", e);
        }
        return key;
    }

    public StoredImage retrieve(String key) {
        try (ResponseInputStream<GetObjectResponse> response = s3Client.getObject(
                GetObjectRequest.builder().bucket(properties.getBucket()).key(key).build())) {
            return new StoredImage(response.readAllBytes(), response.response().contentType());
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to read stored image", e);
        }
    }

    public void delete(String key) {
        s3Client.deleteObject(DeleteObjectRequest.builder().bucket(properties.getBucket()).key(key).build());
    }

    private static String extensionOf(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        return dotIndex >= 0 ? filename.substring(dotIndex) : "";
    }
}
