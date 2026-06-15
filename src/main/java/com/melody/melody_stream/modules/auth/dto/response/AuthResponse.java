package com.melody.melody_stream.modules.auth.dto.response;

import java.util.List;

public record AuthResponse(
        String userId,
        String username,
        String email,
        List<String> roles,
        TokenPair tokens
) {}
