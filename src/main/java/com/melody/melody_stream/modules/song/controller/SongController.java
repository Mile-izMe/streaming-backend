package com.melody.melody_stream.modules.song.controller;

import com.melody.melody_stream.core.dto.response.CursorPage;
import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.song.dto.*;
import com.melody.melody_stream.modules.song.service.SongService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Arrays;
import java.util.stream.Collectors;

@RestController
@RequestMapping("api/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;
    private final MinioBuildService minioBuildService;

    @PostMapping("/presign")
    public ResponseEntity<SongPresignResponse> getPresignUrl(
            @RequestBody @Valid SongPresignRequest request
    ) {
        return ResponseEntity.ok(songService.getPresignUrl(request));
    }

    @PostMapping("metadata")
    public ResponseEntity<SongSaveResponse> saveSong(
            @RequestBody @Valid SongSaveRequest request,
            @AuthenticationPrincipal JwtPayload userDetails
    ) {

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(songService.saveSong(request, userDetails.sub()));
    }

    // ── GET /api/songs?cursor=...&size=20 ────────────────────
    @GetMapping
    public ResponseEntity<CursorPage<SongResponse>> getSongs(
            @RequestParam(required = false) String cursor,
            @RequestParam(defaultValue = "20") int size
    ) {
        return ResponseEntity.ok(songService.getSongs(cursor, size));
    }

    // ── GET /api/songs/{id} ───────────────────────────────────
    @GetMapping("/{id}")
    public ResponseEntity<SongResponse> getSong(@PathVariable String id) {
        return ResponseEntity.ok(songService.getSong(id));
    }

    // ── GET /api/songs/stream/{songId}/** ─────────────────────
    // Proxy HLS — presign redirect for master.m3u8 & all segment .ts
    @GetMapping("/stream/{songId}/**")
    public ResponseEntity<?> streamHls(
            @PathVariable String songId,
            HttpServletRequest request
    ) {
        String fullPath = request.getRequestURI();
        String prefix = "/api/songs/stream/" + songId + "/";
        String hlsPath = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());
        String objectKey = String.format("processed/songs/%s/%s", songId, hlsPath);

        // If sub-playlist → rewrite then response content
        if (hlsPath.endsWith(".m3u8")) {
            String content = minioBuildService.getContent(objectKey);
            // Base proxy for sub-playlist must contain folder
            // Ex: 192k/seg_000.ts → /api/songs/stream/{songId}/192k/seg_000.ts
            String folder = hlsPath.contains("/")
                    ? hlsPath.substring(0, hlsPath.lastIndexOf("/"))
                    : "";
            String baseProxy = "/api/songs/stream/" + songId + (folder.isEmpty() ? "" : "/" + folder);
            return ResponseEntity.ok()
                    .header("Content-Type", "application/vnd.apple.mpegurl")
                    .header("Cache-Control", "no-cache")
                    .body(rewriteM3u8(content, baseProxy));
        }

        // If .ts segment → presign redirect
        String presignUrl = minioBuildService.buildSignedGetUrl(objectKey, 300);
        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", presignUrl)
                .header("Cache-Control", "no-cache")
                .build();
    }

    // ── GET /api/songs/stream/{songId}/master.m3u8 ───────────────
    // Rewrite relative URLs for master.m3u8 → proxy URLs
    @GetMapping("/stream/{songId}/master.m3u8")
    public ResponseEntity<String> streamMaster(@PathVariable String songId) {
        String objectKey = String.format("processed/songs/%s/master.m3u8", songId);
        String content = minioBuildService.getContent(objectKey);
        String baseProxy = "/api/songs/stream/" + songId;
        return ResponseEntity.ok()
                .header("Content-Type", "application/vnd.apple.mpegurl")
                .header("Cache-Control", "no-cache")
                .body(rewriteM3u8(content, baseProxy));
    }

    private String rewriteM3u8(String content, String baseProxy) {
        return Arrays.stream(content.split("\n"))
                .map(line -> {
                    String trimmed = line.trim();
                    if (trimmed.endsWith(".m3u8") || trimmed.endsWith(".ts")) {
                        return baseProxy + "/" + trimmed;
                    }
                    return line;
                })
                .collect(Collectors.joining("\n"));
    }
}
