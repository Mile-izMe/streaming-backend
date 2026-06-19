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
    public ResponseEntity<Void> streamHls(
            @PathVariable String songId,
            HttpServletRequest request
    ) {
        // Extract Path after /stream/{songId}/
        String fullPath = request.getRequestURI();
        String prefix = "/api/songs/stream/" + songId + "/";
        String hlsPath = fullPath.substring(fullPath.indexOf(prefix) + prefix.length());

        String objectKey = String.format("processed/songs/%s/%s", songId, hlsPath);
        String presignUrl = minioBuildService.buildSignedGetUrl(objectKey, 300); // 5 mins

        return ResponseEntity.status(HttpStatus.FOUND)
                .header("Location", presignUrl)
                .header("Cache-Control", "no-cache")
                .build();
    }
}
