package com.melody.melody_stream.modules.song.controller;

import com.melody.melody_stream.modules.song.dto.SongPresignRequest;
import com.melody.melody_stream.modules.song.dto.SongPresignResponse;
import com.melody.melody_stream.modules.song.dto.SongSaveRequest;
import com.melody.melody_stream.modules.song.dto.SongSaveResponse;
import com.melody.melody_stream.modules.song.service.SongService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("api/v1/songs")
@RequiredArgsConstructor
public class SongController {

    private final SongService songService;

    @PostMapping("/presign")
    public ResponseEntity<SongPresignResponse> getPresignUrl(
            @RequestBody @Valid SongPresignRequest request
    ) {
        return ResponseEntity.ok(songService.getPresignUrl(request));
    }

    @PostMapping
    public ResponseEntity<SongSaveResponse> saveSong(
            @RequestBody @Valid SongSaveRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(songService.saveSong(request));
    }
}
