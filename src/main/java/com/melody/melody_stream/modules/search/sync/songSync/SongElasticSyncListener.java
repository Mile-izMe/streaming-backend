package com.melody.melody_stream.modules.search.sync.songSync;

import com.melody.melody_stream.core.event.SongChangedEvent;
import com.melody.melody_stream.modules.search.document.SongDocument;
import com.melody.melody_stream.modules.song.entity.Song;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.time.Instant;
import java.time.LocalDate;

@Slf4j
@Component
@RequiredArgsConstructor
public class SongElasticSyncListener {

    private final ElasticsearchOperations elasticsearchOperations;

    // RUN AFTER DB COMMITED
    @Async
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleSongChangedEvent(SongChangedEvent event) {
        Song song = event.getSong();

        // CASE 1: The Deletion Problem
        if (song.getDeletedAt() != null) {
            log.info("Delete song from Elasticsearch: {}", song.getId());
            elasticsearchOperations.delete(song.getId(), SongDocument.class);
            return;
        }

        // CASE 2: Sync real-time
        log.info("Syncing song into Elasticsearch: {}", song.getId());
        SongDocument doc = convertToDocument(song);
        elasticsearchOperations.save(doc);
    }

    private SongDocument convertToDocument(Song song) {
        // Map data from Entity to Document
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
