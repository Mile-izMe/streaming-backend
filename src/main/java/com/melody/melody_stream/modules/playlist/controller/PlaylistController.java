package com.melody.melody_stream.modules.playlist.controller;

import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.playlist.dto.PlaylistRequest;
import com.melody.melody_stream.modules.playlist.dto.PlaylistResponse;
import com.melody.melody_stream.modules.playlist.service.PlaylistService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import okhttp3.Response;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("api/playlists")
@RequiredArgsConstructor
public class PlaylistController {

    private final PlaylistService playlistService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaylistResponse> create(
            @Valid @ModelAttribute PlaylistRequest request,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(playlistService.create(request, principal.sub()));
    }

    @GetMapping
    public ResponseEntity<List<PlaylistResponse>> getUserPlaylists(
            @AuthenticationPrincipal JwtPayload principal
    ) {
        return ResponseEntity.ok(playlistService.getUserPlaylists(principal.sub()));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PlaylistResponse> getDetail(
            @PathVariable String id,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        return ResponseEntity.ok(playlistService.getDetail(id, principal.sub()));
    }

    @PutMapping(value = "/{id}", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<PlaylistResponse> update(
            @PathVariable String id,
            @Valid @ModelAttribute PlaylistRequest request,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        return ResponseEntity.ok(playlistService.update(id, request, principal.sub()));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @PathVariable String id,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        playlistService.delete(id, principal.sub());
        return ResponseEntity.noContent().build();
    }

    @PostMapping("/{id}/songs")
    public ResponseEntity<Void> addSong(
            @PathVariable String id,
            @RequestParam String songId,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        playlistService.addSong(id, songId, principal.sub());
        return ResponseEntity.noContent().build();
    }

    @DeleteMapping("/{id}/songs/{songId}")
    public ResponseEntity<Void> removeSong(
            @PathVariable String id,
            @PathVariable String songId,
            @AuthenticationPrincipal JwtPayload principal
    ) {
        playlistService.removeSong(id, songId, principal.sub());
        return ResponseEntity.noContent().build();
    }
}
