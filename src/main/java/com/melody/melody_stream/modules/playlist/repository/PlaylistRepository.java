package com.melody.melody_stream.modules.playlist.repository;

import com.melody.melody_stream.modules.playlist.entity.Playlist;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistRepository extends JpaRepository<Playlist, String> {
    List<Playlist> findByUserIdOrderByCreatedAtDesc(String userId);
}
