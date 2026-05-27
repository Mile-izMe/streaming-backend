package com.melody.melody_stream.modules.song.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class SongPresignRequest {
    @NotBlank
    private String fileName;

    @NotBlank
    private String contentType;
}
