package com.melody.melody_stream.infrastructure.minio.service;

import io.minio.GetObjectArgs;
import io.minio.MinioClient;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import software.amazon.awssdk.services.s3.model.GetObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;

@Service
@RequiredArgsConstructor
public class MinioBuildService {

    private final S3Presigner s3Presigner;
    private final MinioClient minioClient;

    @Value("${spring.minio.bucket-name}")
    private String bucket;

    private static final int MAX_EXPIRATION_SECONDS = 7 * 24 * 60 * 60;

    private int normalizeExpiration(int seconds) {
        return Math.min(Math.max(seconds, 1), MAX_EXPIRATION_SECONDS);
    }

    public String buildSignedGetUrl(String key, int expirationSeconds) {
        GetObjectRequest getObjectRequest = GetObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .build();

        GetObjectPresignRequest presignRequest = GetObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(expirationSeconds))
                .getObjectRequest(getObjectRequest)
                .build();

        return s3Presigner.presignGetObject(presignRequest).url().toString();
    }

    public String buildSignedPutUrl(String key, String contentType, int expirationSeconds) {
        PutObjectRequest putObjectRequest = PutObjectRequest.builder()
                .bucket(bucket)
                .key(key)
                .contentType(contentType)
                .build();

        PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
                .signatureDuration(Duration.ofSeconds(normalizeExpiration(expirationSeconds)))
                .putObjectRequest(putObjectRequest)
                .build();

        return s3Presigner.presignPutObject(presignRequest).url().toString();
    }

    public String getContent(String key) {
        try (InputStream is = minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucket)
                        .object(key)
                        .build()
        )) {
            return new String(is.readAllBytes(), StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("Failed to get content: " + key, e);
        }
    }
}
