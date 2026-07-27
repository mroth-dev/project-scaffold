package com.example.scaffold.storage;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import lombok.Data;

/**
 * Connection settings for the S3-compatible object store (Garage) used for
 * uploaded images.
 */
@Data
@Configuration
@ConfigurationProperties(prefix = "app.storage")
public class StorageProperties {

    private String endpoint;
    private String accessKey;
    private String secretKey;
    private String bucket;
    private String region = "garage";
}
