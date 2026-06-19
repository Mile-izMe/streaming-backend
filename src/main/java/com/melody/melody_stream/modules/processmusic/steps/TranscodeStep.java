package com.melody.melody_stream.modules.processmusic.steps;

import com.melody.melody_stream.infrastructure.ffmpeg.service.FfmpegService;
import com.melody.melody_stream.modules.processmusic.ProcessMusicContext;
import com.melody.melody_stream.modules.processmusic.types.ProcessMusicStep;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Service
@Slf4j
@RequiredArgsConstructor
public class TranscodeStep implements ProcessMusicStep {

    private final FfmpegService ffmpegService;

    @Override
    public int stepIndex() {
        return 2;
    }

    @Override
    public String stepName() {
        return "TRANSCODE";
    }

    @Override
    public void process(ProcessMusicContext context) {
        String localTempPath = context.getLocalFilePath();
        String outputDir = context.getHlsOutputDir();

        if (localTempPath == null || localTempPath.trim().isEmpty() ||
            outputDir == null || outputDir.trim().isEmpty()) {
            throw new IllegalArgumentException("Missing localTempPath or outputDir in context");
        }

        try {
            log.info("Starting transcode step. Input: {}, Output Dir: {}", localTempPath, outputDir);

            // 1. Call FfmpegService to convert into HLS
            // This process wait until ffmpeg complete process all bitrates
            String masterPlaylistPath = ffmpegService.convertToHls(localTempPath, outputDir);

            // 2. Store path of master playlist into context for next step (Upload MinIO)
             context.setMasterPlaylistKey(masterPlaylistPath);

            log.info("Transcode step completed successfully. Master playlist generated at: {}", masterPlaylistPath);

        } catch (Exception error) {
            log.error("Transcode step failed", error);

            throw new RuntimeException("Transcode step failed: " + error.getMessage(), error);
        }
    }
}