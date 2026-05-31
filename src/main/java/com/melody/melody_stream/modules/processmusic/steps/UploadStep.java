package com.melody.melody_stream.modules.processmusic.steps;

import com.melody.melody_stream.infrastructure.minio.service.MinioWriteService;
import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@RequiredArgsConstructor
@Slf4j
public class UploadStep implements ProcessMusicStep {

    private final MinioWriteService minioWriteService;

    @Override
    public int stepIndex() {
        return 3;
    }

    @Override
    public String stepName() {
        return "UPLOAD";
    }

    @Override
    public void process(ProcessMusicContext context) {
        String outputDir = context.getHlsOutputDir();
        String songId = context.getSongId();

        if (outputDir == null || outputDir.trim().isEmpty()) {
            throw new IllegalArgumentException("outputDir is not defined in context");
        }

        Path outputDirPath = Paths.get(outputDir);
        if (!Files.exists(outputDirPath)) {
            throw new RuntimeException("Output directory does not exist: " + outputDir);
        }

        try {
            log.info("Starting upload step for Song ID: {} from directory: {}", songId, outputDir);

            // 1. Use Files.walk to recursively scan all folder
            List<Path> files;
            try (Stream<Path> paths = Files.walk(outputDirPath)) {
                files = paths.filter(Files::isRegularFile).collect(Collectors.toList());
            }

            if (files.isEmpty()) {
                log.warn("No files found to upload in directory: {}", outputDir);
                return;
            }

            // 2. Upload each file (.m3u8 & .ts) to MinIO
            for (Path file : files) {
                String fileName = file.getFileName().toString();

                // relativize to get similar path (Ex: from /tmp/out/128k/seg.ts -> 128k/seg.ts)
                Path relativePath = outputDirPath.relativize(file);

                // Replace "\" to "/" for standard format S3 Key even runs on Windows or Linux
                String s3Key = String.format("processed/songs/%d/%s",
                        songId, relativePath.toString().replace("\\", "/"));

                String contentType = fileName.endsWith(".m3u8")
                        ? "application/vnd.apple.mpegurl"
                        : "video/MP2T";

                // Flow read local file & push to MinIO through MinioWriteService
                try (InputStream inputStream = Files.newInputStream(file)) {
                    long fileSize = Files.size(file);

                    minioWriteService.uploadStream(s3Key, inputStream, fileSize, contentType);
                }

                log.debug("Uploaded {} to MinIO as {}", fileName, s3Key);
            }

            log.info("Upload step completed successfully for Song ID: {}. Total files uploaded: {}", songId, files.size());


        } catch (Exception error) {
            log.error("Upload step failed for Song ID: {}", songId, error);
            throw new RuntimeException("Upload step failed: " + error.getMessage(), error);
        }
    }
}
