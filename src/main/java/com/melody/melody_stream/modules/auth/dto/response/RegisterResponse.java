package com.melody.melody_stream.modules.auth.dto.response;

public record RegisterResponse(
        String id,
        String username,
        String email,
        String message    // "Verification email sent"
) {}
 