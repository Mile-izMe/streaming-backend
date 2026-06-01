package com.melody.melody_stream.modules.auth.dto.request;

import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @NotBlank String email,
        @NotBlank String password,
        String deviceId   // nullable — server generates one if absent
) {}
