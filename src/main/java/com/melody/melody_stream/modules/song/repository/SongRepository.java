package com.melody.melody_stream.modules.song.repository;

import com.melody.melody_stream.modules.song.entity.Song;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SongRepository extends JpaRepository<Song, String> {
}
