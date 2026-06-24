package com.melody.melody_stream.modules.search.sync.songSync;

import com.melody.melody_stream.modules.search.document.SongDocument;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class SongSyncCronJob {

    private final SongRepository songRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    // Save last run time -> This var should be stored in Redis or table in DB
    private LocalDateTime lastWaterMark = LocalDateTime.of(1970, 1, 1, 0, 0);

    // 30 minutes a time
    @Scheduled(fixedDelay = 1800000)
    public void syncMissedSongsToElastic() {
        log.info("Started CronJob for Elasticsearch from watermark: {}", lastWaterMark);

        // 1. Get current time to make new watermark
        LocalDateTime currentRunTime = LocalDateTime.now();

        // 2. Query songs that have CHANGES from last run time
        List<Song> changedSongs = songRepository.findByUpdatedAtAfter(lastWaterMark);

        if (changedSongs.isEmpty()) {
            lastWaterMark = currentRunTime;
            return;
        }

        // 3. Categorize and push by Batch
        for (Song song : changedSongs) {
            try {
                if (song.getDeletedAt() != null) {
                    elasticsearchOperations.delete(song.getId(), SongDocument.class);
                } else {
                    elasticsearchOperations.save(convertToDocument(song));
                }
            } catch (Exception e) {
                log.error("Error syncing song {}: {}", song.getId(), e.getMessage());
                return;
            }
        }

        // 4. Update Watermark after syncing successfully
        lastWaterMark = currentRunTime;
        log.info("Cronjob finished. Synced {} song", changedSongs.size());
    }

    private SongDocument convertToDocument(Song song) {
        return SongDocument.builder()
                .id(song.getId())
                .title(song.getTitle())
                .artist(song.getArtist())
                .status(String.valueOf(song.getStatus()))
                .thumbnailUrl(song.getThumbnailUrl())
                .audioUrl(song.getAudioUrl())
                .duration(song.getDuration())
                .createdAt(LocalDate.from(song.getCreatedAt()))
                .updatedAt(LocalDate.from(song.getUpdatedAt()))
                .build();
    }

}
