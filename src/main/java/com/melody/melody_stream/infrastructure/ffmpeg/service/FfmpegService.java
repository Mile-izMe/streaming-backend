package com.melody.melody_stream.infrastructure.ffmpeg.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
public class FfmpegService {

    private final List<String> audioBitrates = List.of("64k", "96k", "128k", "192k");

    private record Variant(String bitrate, int bandwidth) {}

    /**
     * Transcode file into HLS format (.m3u8)
     */
    public String convertToHls(String inputPath, String outputDir) throws IOException, InterruptedException {
        Path outputDirPath = Paths.get(outputDir);

        if (!Files.exists(outputDirPath)) {
            Files.createDirectories(outputDirPath);
        }

        List<Variant> variants = new ArrayList<>();

        for (String bitrate : audioBitrates) {
            int bandwidth = bitrateToBandwidth(bitrate);
            variants.add(new Variant(bitrate, bandwidth));

            Path variantDir = outputDirPath.resolve(bitrate);
            if (!Files.exists(variantDir)) {
                Files.createDirectories(variantDir);
            }

            Path playlistPath = variantDir.resolve("playlist.m3u8");

            // Process each bitrate (use CompletableFuture for parallel)
            transcodeVariant(inputPath, playlistPath.toString(), variantDir.toString(), bitrate);
        }

        Path masterPlaylistPath = outputDirPath.resolve("master.m3u8");
        String masterPlaylist = buildMasterPlaylist(variants);
        Files.writeString(masterPlaylistPath, masterPlaylist);

        return masterPlaylistPath.toString();
    }

    /**
     * Execute FFmpeg through ProcessBuilder
     */
    private void transcodeVariant(String inputPath, String outputPath, String outputDir, String bitrate)
            throws IOException, InterruptedException {

        String segmentFilename = Paths.get(outputDir, "seg_%03d.ts").toString();

        List<String> command = List.of(
                "ffmpeg",
                "-y", // Overwrite file if exits
                "-i", inputPath,
                "-c:a", "aac",
                "-b:a", bitrate,
                "-vn", // Pass video
                "-ac", "2",
                "-ar", "44100",
                "-hls_time", "6",
                "-hls_playlist_type", "vod",
                "-hls_list_size", "0",
                "-hls_segment_filename", segmentFilename,
                outputPath
        );

        log.info("Spawned FFmpeg ({}) with command: {}", bitrate, String.join(" ", command));

        ProcessBuilder processBuilder = new ProcessBuilder(command);
        processBuilder.redirectErrorStream(true); // Combine stderr into stdout to read log

        Process process = processBuilder.start();

        // Read output of ffmpeg to avoid buffer cause loading process
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
            String line;
            while ((line = reader.readLine()) != null) {
                // Set log at DEBUG/TRACE to watch detail progress event of node
                log.trace("[FFmpeg - {}]: {}", bitrate, line);
            }
        }

        int exitCode = process.waitFor();
        if (exitCode != 0) {
            log.error("Error transcoding {}", bitrate);
            throw new RuntimeException("FFmpeg process failed with exit code: " + exitCode);
        }

        log.info("Transcoding finished for {}!", bitrate);
    }

    /**
     * Create content for Master Playlist
     */
    private String buildMasterPlaylist(List<Variant> variants) {
        StringBuilder sb = new StringBuilder();
        sb.append("#EXTM3U\n");
        sb.append("#EXT-X-VERSION:3\n");

        for (Variant variant : variants) {
            sb.append(String.format("#EXT-X-STREAM-INF:BANDWIDTH=%d,CODECS=\"mp4a.40.2\"\n", variant.bandwidth()));
            sb.append(String.format("%s/playlist.m3u8\n", variant.bitrate()));
        }

        return sb.toString();
    }

    /**
     * Convert "64k" into 64000
     */
    private int bitrateToBandwidth(String bitrate) {
        String numericString = bitrate.replaceAll("[^0-9]", "");
        try {
            int numeric = Integer.parseInt(numericString);
            return numeric * 1000;
        } catch (NumberFormatException e) {
            return 192000; // Default fallback value
        }
    }
}
