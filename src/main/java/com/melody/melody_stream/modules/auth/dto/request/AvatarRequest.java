package com.melody.melody_stream.modules.auth.dto.request;

import org.springframework.web.multipart.MultipartFile;

public record AvatarRequest (
    String userId,
    MultipartFile file
) {}
