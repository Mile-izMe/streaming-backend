package com.melody.melody_stream.infrastructure.minio.service;

import io.minio.*;
import io.minio.messages.Item;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

@Service
public class MinioReadService {

    private final MinioClient minioClient;
    private final ObjectMapper objectMapper;
    @Value("${spring.minio.bucket-name}")
    private String bucketName;

    public MinioReadService(MinioClient minioClient, ObjectMapper objectMapper) {
        this.minioClient = minioClient;
        this.objectMapper = objectMapper;
    }


    /**
     * Return InputStream.
     */
    public InputStream stream(String key) throws Exception {
        return minioClient.getObject(
                GetObjectArgs.builder()
                        .bucket(bucketName)
                        .object(key)
                        .build()
        );
    }

    /**
     * Return byte array.
     */
    public byte[] getBytes(String key) throws Exception {
        try (InputStream stream = stream(key)) {
            return stream.readAllBytes();
        }
    }

    /**
     * Return Object. Equivalent to JSON parsing with SuperJSON.
     */
    public <T> T getJson(String key, Class<T> clazz) {
        try {
            byte[] data = getBytes(key);
            return objectMapper.readValue(data, clazz);
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Check if file exists using StatObject (Equivalent to HeadObject).
     */
    public boolean exists(String key) {
        try {
            minioClient.statObject(
                    StatObjectArgs.builder()
                            .bucket(bucketName)
                            .object(key)
                            .build()
            );
            return true;
        } catch (Exception e) {
            return false;
        }
    }

    /**
     * List contents.
     */
    public List<String> list(String key) {
        String prefix = key.endsWith("/") ? key : key + "/";

        Iterable<Result<Item>> results = minioClient.listObjects(
                ListObjectsArgs.builder()
                        .bucket(bucketName)
                        .prefix(prefix)
                        .delimiter("/")
                        .build()
        );

        return StreamSupport.stream(results.spliterator(), false)
                .map(result -> {
                    try {
                        return result.get().objectName().replace(prefix, "").replace("/", "");
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(name -> name != null && !name.isEmpty())
                .collect(Collectors.toList());
    }
}
