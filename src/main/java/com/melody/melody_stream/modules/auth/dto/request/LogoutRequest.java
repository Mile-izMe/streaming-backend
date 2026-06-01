package com.melody.melody_stream.modules.auth.dto.request;

public record LogoutRequest(
        String deviceId   // null = logout current device; handled in controller
) {}