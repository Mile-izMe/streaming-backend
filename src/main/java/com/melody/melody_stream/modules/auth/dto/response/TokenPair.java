package com.melody.melody_stream.modules.auth.dto.response;

public record TokenPair(
        String accessToken,
        String refreshToken
) {}