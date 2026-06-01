package com.melody.melody_stream.modules.auth.dto.response;

import java.util.List;

public record JwtPayload(
        String sub,           // userId
        String username,
        List<String> roles,
        List<String> permissions,
        String deviceId
) {}