package com.melody.melody_stream.modules.playlist.dto;

import com.melody.melody_stream.modules.song.dto.SongResponse;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data @Builder
public class PlaylistResponse {
    private String id;
    private String name;
    private String description;
    private String thumbnailUrl;
    private int songCount;
    private List<SongResponse> songs;
    private LocalDateTime createdAt;
}
