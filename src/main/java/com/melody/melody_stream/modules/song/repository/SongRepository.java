package com.melody.melody_stream.modules.song.repository;

import com.melody.melody_stream.modules.song.entity.Song;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface SongRepository extends JpaRepository<Song, String> {
    @Query("""
        SELECT s from Song s
        WHERE s.status = SongStatus.COMPLETED
        ORDER BY s.createdAt DESC
        """)
    List<Song> findAllCompleted(Pageable pageable);

    @Query("""
        SELECT s from Song s 
        WHERE s.status = SongStatus.COMPLETED
        AND s.createdAt < :cursor
        ORDER BY s.createdAt DESC
        """)
    List<Song> findBeforeCursor(
         @Param("cursor")Instant cursor,
         Pageable pageable
    );
}
