package com.melody.melody_stream.config;

import io.minio.MinioClient;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MinioConfig {

    @Bean
    public MinioClient minioClien() {
        return MinioClient.builder()
                .endpoint("http://localhost:9000")
                .credentials("melody", "melody123")
                .build();
    }
}
