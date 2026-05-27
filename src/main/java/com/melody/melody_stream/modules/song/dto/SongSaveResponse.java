package com.melody.melody_stream.modules.song.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SongSaveResponse {
    private String id;
    private String title;
    private String artist;
    private String audioUrl;
    private String thumbnailUrl;
}