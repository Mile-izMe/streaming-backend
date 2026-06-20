package com.melody.melody_stream.modules.song.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SongResponse {
    private String id;
    private String title;
    private String artist;
    private String audioUrl;
    private String[] lyrics;
    private String thumbnailUrl;
    private String status;
    private Integer duration;
}