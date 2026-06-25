package com.melody.melody_stream.modules.playlist.repository;

import com.melody.melody_stream.modules.playlist.entity.PlayListSongId;
import com.melody.melody_stream.modules.playlist.entity.PlaylistSong;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PlaylistSongRepository extends JpaRepository<PlaylistSong, PlayListSongId> {
    boolean existsById(PlayListSongId id);
    void deleteById(PlayListSongId id);
    List<PlaylistSong> findByIdPlaylistIdOrderByCreatedAtDesc(String playlistId);
}
