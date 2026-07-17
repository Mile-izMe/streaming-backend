package com.melody.melody_stream.modules.song.service;

import com.melody.melody_stream.core.constant.ActionType;
import com.melody.melody_stream.core.dto.response.CursorPage;
import com.melody.melody_stream.core.enums.JobStatus;
import com.melody.melody_stream.core.exception.ForbiddenException;
import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.infrastructure.minio.service.MinioWriteService;
import com.melody.melody_stream.modules.job.entity.Job;
import com.melody.melody_stream.modules.job.repository.JobRepository;
import com.melody.melody_stream.modules.processmusic.ProcessMusicPublisher;
import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import com.melody.melody_stream.modules.song.dto.*;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.ResourceNotFoundException;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SongService {

    private final MinioWriteService minioWriteService;
    private final MinioBuildService minioBuildService;
    private final SongRepository songRepository;
    private final JobRepository jobRepository;
    private final ProcessMusicPublisher processMusicPublisher;

    // ── Save song ────────────────────────────────────────────────
    @Transactional
    public SongSaveResponse saveSong(SongSaveRequest request, String userId) {
        Song song = Song.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .audioUrl(request.getObjectKey())
                .thumbnailUrl(request.getThumbnailUrl())
                .lyrics(request.getLyrics())
                .build();

        song = songRepository.save(song);

        Map<String, Object> payload = new HashMap<>();
        payload.put("songId", song.getId());
        payload.put("objectKey", request.getObjectKey());

        Job job = Job.builder()
                .actionType(ActionType.PROCESS_MUSIC)
                .status(JobStatus.PENDING)
                .payload(payload)
                .currentStep(0)
                .maxStep(5)
                .userId(userId)
                .build();
        job = jobRepository.save(job);

        ProcessMusicMessage message = new ProcessMusicMessage(
                job.getId(),
                song.getId(),
                userId,
                1
        );
        processMusicPublisher.publish(message);

        return SongSaveResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .audioUrl(song.getAudioUrl())
                .thumbnailUrl(song.getThumbnailUrl())
                .lyrics(song.getLyrics())
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

    // ── Get song ────────────────────────────────────────────────
    public CursorPage<SongResponse> getSongs(String cursor, int size) {
        Pageable pageable = PageRequest.of(0, size + 1); // +1 để check hasMore

        List<Song> songs = cursor == null
                ? songRepository.findAllCompleted(pageable)
                : songRepository.findBeforeCursor(Instant.parse(cursor), pageable);

        boolean hasMore = songs.size() > size;
        if (hasMore) songs = songs.subList(0, size);

        String nextCursor = hasMore
                ? songs.get(songs.size() - 1).getCreatedAt().toString()
                : null;

        return new CursorPage<>(
                songs.stream().map(this::toResponse).toList(),
                nextCursor,
                hasMore
        );
    }

    public SongResponse getSong(String id) {
        Song song = songRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Song not found"));
        return toResponse(song);
    }

    // ── Update song ────────────────────────────────────────────────
    public SongResponse update(String songId, SongSaveRequest request, String userId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        if (!song.getCreatedBy().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        song.setTitle(request.getTitle());
        song.setArtist(request.getArtist());
        song.setLyrics(request.getLyrics());

        String newThumbnailKey = request.getThumbnailUrl();
        if (newThumbnailKey != null && !newThumbnailKey.trim().isEmpty()) {
             if (song.getThumbnailUrl() != null && !song.getThumbnailUrl().equals(newThumbnailKey)) {
                 try {
                     minioWriteService.deleteFile(song.getThumbnailUrl());
                 } catch (Exception e) {
                     log.warn("Failed to delete old song thumbnail: {}", e.getMessage());
                 }
             }

            song.setThumbnailUrl(newThumbnailKey);
        }

        song = songRepository.save(song);

        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .audioUrl(song.getAudioUrl())
                .thumbnailUrl(song.getThumbnailUrl())
                .lyrics(song.getLyrics())
                .build();
    }

    // ── Delete song ────────────────────────────────────────────────
    public void delete(String songId, UUID userId) {
        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new ResourceNotFoundException("Song not found"));

        if (!song.getCreatedBy().equals(userId)) {
            throw new ForbiddenException("You are not the owner of this song.");
        }

        songRepository.softDelete(songId, userId);
    }

    private SongResponse toResponse(Song song) {
        String thumbnailUrl = song.getThumbnailUrl() != null
                ? minioBuildService.buildSignedGetUrl(song.getThumbnailUrl(), 3600) // 1 giờ
                : null;

        return SongResponse.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .audioUrl(song.getAudioUrl())
                .thumbnailUrl(thumbnailUrl)
                .status(song.getStatus().name())
                .duration(song.getDuration())
                .lyrics(song.getLyrics())
                .build();
    }
}
