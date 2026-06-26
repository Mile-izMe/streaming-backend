package com.melody.melody_stream.modules.playlist.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;
import org.springframework.web.multipart.MultipartFile;

@Data
public class PlaylistRequest {
    @NotBlank
    private String name;

    private String description;

    private MultipartFile thumbnailUrl;
}
