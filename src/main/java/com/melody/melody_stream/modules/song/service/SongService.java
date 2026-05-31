package com.melody.melody_stream.modules.song.service;

import com.melody.melody_stream.core.constant.ActionType;
import com.melody.melody_stream.entity.enums.JobStatus;
import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.modules.job.entity.Job;
import com.melody.melody_stream.modules.job.repository.JobRepository;
import com.melody.melody_stream.modules.processmusic.ProcessMusicPublisher;
import com.melody.melody_stream.modules.processmusic.message.ProcessMusicMessage;
import com.melody.melody_stream.modules.song.dto.SongPresignRequest;
import com.melody.melody_stream.modules.song.dto.SongPresignResponse;
import com.melody.melody_stream.modules.song.dto.SongSaveRequest;
import com.melody.melody_stream.modules.song.dto.SongSaveResponse;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class SongService {

    private final MinioBuildService minioBuildService;
    private final SongRepository songRepository;
    private final JobRepository jobRepository;
    private final ProcessMusicPublisher processMusicPublisher;

    @Transactional
    public SongSaveResponse saveSong(SongSaveRequest request) {
        Song song = Song.builder()
                .title(request.getTitle())
                .artist(request.getArtist())
                .audioUrl(request.getObjectKey())
                .thumbnailUrl(request.getThumbnailUrl())
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
//                .userId(request.getUserId)
                .build();
        job = jobRepository.save(job);

        ProcessMusicMessage message = new ProcessMusicMessage(
                job.getId(),
                song.getId(),
                "USER_ID",
                1
        );
        processMusicPublisher.publish(message);

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
