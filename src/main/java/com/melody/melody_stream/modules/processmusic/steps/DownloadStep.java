package com.melody.melody_stream.modules.processmusic.steps;

import com.melody.melody_stream.infrastructure.minio.service.MinioReadService;
import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;

@Slf4j
@Component
@RequiredArgsConstructor
public class DownloadStep implements ProcessMusicStep {

    private final MinioReadService minioReadService;

    @Override
    public int stepIndex() {
        return 0;
    }

    @Override
    public String stepName() {
        return "DOWNLOAD";
    }

    @Override
    public void process(ProcessMusicContext context) {
        String audioUrl = context.getObjectKey();
        String localTempPath = context.getLocalFilePath();

        if (localTempPath == null || localTempPath.trim().isEmpty()) {
            throw new IllegalArgumentException("LocalTempPath is not defined in context");
        }

        try {
            Path path = Paths.get(localTempPath);

            if (path.getParent() != null && !Files.exists(path.getParent())) {
                Files.createDirectories(path.getParent());
                log.info("Created directory: {}", path.getParent());
            }

            log.info("Starting download from MinIO: {} to {}", audioUrl, localTempPath);
            try (InputStream inputStream = minioReadService.stream(audioUrl)) {
                // Sử dụng Files.copy để pipe dữ liệu an toàn từ Stream xuống Disk
                Files.copy(inputStream, path, StandardCopyOption.REPLACE_EXISTING);
            }
            log.info("Download completed successfully.");
        } catch (Exception error) {
            log.error("Download step failed", error);
            // Ném lỗi RuntimeException để quá trình xử lý Job phía trên (như BullMQ / Kafka listener) có thể catch và retry
            throw new RuntimeException("Download step failed: " + error.getMessage(), error);
        }
    }
}
