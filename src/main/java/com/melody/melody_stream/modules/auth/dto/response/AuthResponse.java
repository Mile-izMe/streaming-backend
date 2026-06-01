package com.melody.melody_stream.modules.auth.dto.response;

import java.util.List;

public record AuthResponse(
        String userId,
        String username,
        List<String> roles,
        TokenPair tokens
) {}
