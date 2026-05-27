package com.melody.melody_stream.modules.song.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class SongPresignResponse {
    private String objectKey;
    private String presignUrl;
}
