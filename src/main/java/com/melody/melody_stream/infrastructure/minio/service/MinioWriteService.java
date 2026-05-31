package com.melody.melody_stream.infrastructure.minio.service;

import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.util.List;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
@Slf4j
public class MinioWriteService {

    private final MinioClient minioClient;
    private final MinioReadService minioReadService;
    private final ObjectMapper objectMapper;

    @Value("${spring.minio.bucket-name}")
    private String bucketName;

    /**
     * Interface support to get Hash to compare to optimize when uploading JSON
     */
    public interface HashablePayload {
        String getHash();
    }

    /**
     * Upload JSON with Hash for optimization
     * Java is Strong-typed so T need to implement HashablePayload
     */
    public <T extends HashablePayload> void uploadJson(String key, T payload) {
        try {
            // 1. Read old data to fast compare hash (Class of current payload)
            @SuppressWarnings("unchecked")
            T existingData = (T) minioReadService.getJson(key, payload.getClass());

            // 2. If existing data and hash does not change then pass over upload
            if (existingData != null && existingData.getHash().endsWith(payload.getHash())) {
                log.debug("Hash matched for key {}, skipping JSON upload", key);
                return;
            }

            // 3. Serialize object into JSON byte array
            byte[] jsonData = objectMapper.writeValueAsBytes(payload);

            // 4. Upload to MinIO
            try (ByteArrayInputStream bais = new ByteArrayInputStream(jsonData)) {
                minioClient.putObject(
                        PutObjectArgs.builder()
                                .bucket(bucketName)
                                .object(key)
                                .stream(bais, jsonData.length, -1)
                                .contentType("application/json")
                                .build()
                );
            }
            log.info("Upload JSON successfully to key: {}", key);
        } catch (Exception error) {
            log.error("[MinioWriteService] failed to upload JSON to {}", key, error);
        }
    }

    /**
     * Upload Buffer (Image, small files, v.v.)
     * Node.js Buffer similar to byte[]
     */
    public void uploadBuffer(String key, byte[] buffer, String contentType) {
        try (ByteArrayInputStream bais = new ByteArrayInputStream(buffer)) {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .stream(bais, buffer.length, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Uploaded buffer to key: {}", key);
        } catch (Exception e) {
            log.error("[MinioWriteService] Failed to upload buffer to {}", key, e);
            throw new RuntimeException("Upload buffer failed", e);
        }
    }

    /**
     * Upload Stream (Use for big file/music)
     */
    public void uploadStream(String key, InputStream stream, long objectSize, String contentType) {
        try {
            minioClient.putObject(
                    PutObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            // objectSize is the size of file (already known), partSize -1 for MinIO to auto calculate
                            .stream(stream, objectSize, -1)
                            .contentType(contentType)
                            .build()
            );
            log.debug("Uploaded stream to key: {}", key);
        } catch (Exception e) {
            log.error("[MinioWriteService] Failed to upload stream to {}", key, e);
            throw new RuntimeException("Upload stream failed", e);
        }
    }

    /**
     * Upload concurrency for many providers
     */
    public <T extends HashablePayload> void uploadJsonAsyncMultipleProviders(String key, T payload, List<String> providers) {
        List<CompletableFuture<Void>> futures = providers.stream()
                .map(provider -> CompletableFuture.runAsync(() -> {
                    // Logic upload for each provider (If configure many different MinioClients)
                    uploadJson(key, payload);
                }).exceptionally(ex -> {
                    log.error("Failed async upload for provider", ex);
                    return null; // Similar to allIgnoreError
                }))
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
