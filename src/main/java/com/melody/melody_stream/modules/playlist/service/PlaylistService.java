package com.melody.melody_stream.modules.playlist.service;

import com.melody.melody_stream.infrastructure.minio.service.MinioBuildService;
import com.melody.melody_stream.infrastructure.minio.service.MinioWriteService;
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
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class PlaylistService {

    private final PlaylistRepository playlistRepository;
    private final PlaylistSongRepository playlistSongRepository;
    private final SongRepository songRepository;
    private final MinioBuildService minioBuildService;
    private final MinioWriteService minioWriteService;

    // ── Create playlist ──────────────────────────────────────────
    public PlaylistResponse create(PlaylistRequest request, String userId) {
        String thumbnailUrl = null;
        MultipartFile thumbnail = request.getThumbnailUrl();

        if (thumbnail != null && !thumbnail.isEmpty()) {
            try {
                String key = String.format("playlist/%s/thumbnail/%s_%s",
                        userId,
                        UUID.randomUUID(),
                        thumbnail.getOriginalFilename()
                );

                minioWriteService.uploadBuffer(key, thumbnail.getBytes(), thumbnail.getContentType());
                thumbnailUrl = key;
            } catch (Exception e) {
                log.warn("Failed to upload thumbnail, continuing without it: {}", e.getMessage());
            }
        }

        Playlist playlist = Playlist.builder()
                .name(request.getName())
                .description(request.getDescription())
                .thumbnailUrl(thumbnailUrl)
                .userId(userId)
                .build();
        return toResponse(playlistRepository.save(playlist), false);
    }

    // ── Get list of user ────────────────────────────────
    public List<PlaylistResponse> getUserPlaylists(String userId, String checkSongId) {
        return playlistRepository.findByUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(playlist -> {
                    boolean isContain = false;

                    if (checkSongId != null && !checkSongId.trim().isEmpty()) {
                        isContain = playlistSongRepository.existsByPlaylistIdAndSongId(playlist.getId(), checkSongId);
                    }
                    return toResponse(playlist, false, isContain);
                })
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
        return toResponse(playlist, includeSongs, false);
    }

    private PlaylistResponse toResponse(Playlist playlist, boolean includeSongs, boolean isContainSong) {
        List<SongResponse> songs = new ArrayList<>();
        int songCount = (playlist.getSongs() != null) ? playlist.getSongs().size() : 0;

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
                                .lyrics(song.getLyrics())
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
                .thumbnailUrl(playlist.getThumbnailUrl() != null
                        ? minioBuildService.buildSignedGetUrl(playlist.getThumbnailUrl(), 3600)
                        : null)
                .songCount(songCount)
                .songs(songs)
                .createdAt(playlist.getCreatedAt())
                .isContainSong(isContainSong)
                .build();
    }
}
