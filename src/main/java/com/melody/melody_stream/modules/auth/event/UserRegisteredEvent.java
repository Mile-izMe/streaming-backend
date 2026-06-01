package com.melody.melody_stream.modules.auth.event;

public record UserRegisteredEvent(
        String email,
        String username,
        String verificationToken
) {}
