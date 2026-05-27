package com.melody.melody_stream.modules.song.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SongSaveRequest {
    @NotBlank
    private String title;

    @NotBlank
    private String artist;

    @NotBlank
    private String objectKey;

    private String thumbnailUrl;
}