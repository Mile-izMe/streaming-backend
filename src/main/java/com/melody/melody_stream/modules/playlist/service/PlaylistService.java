package com.melody.melody_stream.modules.playlist.service;

import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.modules.playlist.dto.PlaylistRequest;
import com.melody.melody_stream.modules.playlist.dto.PlaylistResponse;
import com.melody.melody_stream.modules.playlist.entity.PlayListSongId;
import com.melody.melody_stream.modules.playlist.entity.Playlist;
import com.melody.melody_stream.modules.playlist.entity.PlaylistSong;
import com.melody.melody_stream.modules.playlist.repository.PlaylistRepository;
import com.melody.melody_stream.modules.playlist.repository.PlaylistSongRepository;
import com.melody.melody_stream.modules.song.dto.SongResponse;
import com.melody.melody_stream.modules.song.entity.Song;
import com.melody.melody_stream.modules.song.repository.SongRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final MinioBuildService minioBuildService;

    // ── Create playlist ──────────────────────────────────────────
    public PlaylistResponse create(PlaylistRequest request, String userId) {
        Playlist playlist = Playlist.builder()
                .name(request.getName())
                .description(request.getDescription())
                .userId(userId)
                .build();
        return toResponse(playlistRepository.save(playlist), false);
    }

    // ── Get list of user ────────────────────────────────
    public List<PlaylistResponse> getUserPlaylists(String userId) {
        return playlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(p -> toResponse(p, false))
                .toList();
    }

    // ── Get details + songs ──────────────────────────────────
    public PlaylistResponse getDetail(String playlistId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        return toResponse(playlist, true);
    }

    // ── Update ────────────────────────────────────────────────
    public PlaylistResponse update(String playlistId, PlaylistRequest request, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        playlist.setName(request.getName());
        playlist.setDescription(request.getDescription());
        return toResponse(playlistRepository.save(playlist), false);
    }

    // ── Delete ────────────────────────────────────────────────
    public void delete(String playlistId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        playlistRepository.delete(playlist);
    }

    // ── Add song to playlist ─────────────────────────────────
    @Transactional
    public void addSong(String playlistId, String songId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        Song song = songRepository.findById(songId)
                .orElseThrow(() -> new RuntimeException("Song not found"));

        PlayListSongId id = new PlayListSongId(playlistId, songId);

        if (playlistSongRepository.existsById(id)) return;

        PlaylistSong playlistSong = PlaylistSong.builder()
                .id(id)
                .playlist(playlist)
                .song(song)
                .build();

        playlistSongRepository.save(playlistSong);
    }

    // ──  Remove song from playlist ────────────────────────────────
    @Transactional
    public void removeSong(String playlistId, String songId, String userId) {
        Playlist playlist = playlistRepository.findById(playlistId)
                .orElseThrow(() -> new RuntimeException("Playlist not found"));

        if (!playlist.getUserId().equals(userId)) {
            throw new RuntimeException("Access denied");
        }

        playlistSongRepository.deleteById(new PlayListSongId(playlistId, songId));
    }

    // ── Helper ────────────────────────────────────────────────
    private PlaylistResponse toResponse(Playlist playlist, boolean includeSongs) {
        List<SongResponse> songs = new ArrayList<>();

        if (includeSongs) {
            songs = playlistSongRepository
                    .findByIdPlaylistIdOrderByCreatedAtDesc(playlist.getId())
                    .stream()
                    .map(ps -> {
                        Song song = ps.getSong();
                        return SongResponse.builder()
                                .id(song.getId())
                                .title(song.getTitle())
                                .artist(song.getArtist())
                                .audioUrl(song.getAudioUrl())
                                .thumbnailUrl(song.getThumbnailUrl() != null
                                        ? minioBuildService.buildSignedGetUrl(song.getThumbnailUrl(), 3600)
                                        : null)
                                .duration(song.getDuration())
                                .status(String.valueOf(song.getStatus()))
                                .build();
                    })
                    .toList();
            }

        return PlaylistResponse.builder()
                .id(playlist.getId())
                .name(playlist.getName())
                .description(playlist.getDescription())
                .songCount(playlist.getSongs().size())
                .songs(songs)
                .createdAt(playlist.getCreatedAt())
                .build();
    }
}
