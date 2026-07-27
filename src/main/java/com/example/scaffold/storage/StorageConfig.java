package com.example.scaffold.storage;

import java.net.URI;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.core.checksums.RequestChecksumCalculation;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.S3Configuration;

@Configuration
public class StorageConfig {

    @Bean
    public S3Client s3Client(StorageProperties properties) {
        return S3Client.builder()
                .endpointOverride(URI.create(properties.getEndpoint()))
                .region(Region.of(properties.getRegion()))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(properties.getAccessKey(), properties.getSecretKey())))
                // Garage (and most non-AWS S3-compatible stores) requires path-style
                // bucket addressing instead of AWS's default virtual-hosted-style.
                .forcePathStyle(true)
                // The SDK's newer default - always attaching a flexible checksum
                // (e.g. CRC32) as a signed trailer - makes Garage reject the request
                // with "Invalid payload signature". WHEN_REQUIRED restores the old
                // behavior: only add a checksum when the API operation demands one.
                .requestChecksumCalculation(RequestChecksumCalculation.WHEN_REQUIRED)
                // Belt-and-braces alongside the checksum setting above: forces a
                // single signed payload instead of aws-chunked transfer encoding,
                // which is the other half of what Garage rejects.
                .serviceConfiguration(S3Configuration.builder()
                        .chunkedEncodingEnabled(false)
                        .build())
                .build();
    }
}
