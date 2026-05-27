package com.melody.melody_stream.modules.song.service;

import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.modules.song.dto.SongPresignRequest;
import com.melody.melody_stream.modules.song.dto.SongPresignResponse;
import com.melody.melody_stream.modules.song.dto.SongSaveRequest;
import com.melody.melody_stream.modules.song.dto.SongSaveResponse;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SongService {

    private final MinioBuildService minioBuildService;
    private final SongRepository songRepository;

    @Transactional
    public SongSaveResponse saveSong(SongSaveRequest request) {
        Song song = Song.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .audioUrl(request.getObjectKey())
                .thumbnailUrl(request.getThumbnailUrl())
                .build();

        song = songRepository.save(song);

        return SongSaveResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .audioUrl(song.getAudioUrl())
                .thumbnailUrl(song.getThumbnailUrl())
                .build();
    }

    public SongPresignResponse getPresignUrl(SongPresignRequest request) {
        // Create ObjectKey: songs/<uuid>/<filename>
        String objectKey = String.format("songs/%s/%s", UUID.randomUUID(), request.getFileName());

        String presignUrl = minioBuildService.buildSignedPutUrl(objectKey, request.getContentType(), 100000);

        return SongPresignResponse.builder()
                .objectKey(objectKey)
                .presignUrl(presignUrl)
                .build();
    }
}
