package com.melody.melody_stream.modules.auth.controller;

import com.melody.melody_stream.modules.auth.dto.request.*;
import com.melody.melody_stream.modules.auth.dto.response.AuthResponse;
import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.auth.dto.response.RegisterResponse;
import com.melody.melody_stream.modules.auth.dto.response.TokenPair;
import com.melody.melody_stream.modules.auth.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    // POST /api/auth/register
    @PostMapping("/register")
    @Operation(summary = "Register new account", security = {})
    public ResponseEntity<RegisterResponse> register(
            @Valid @RequestBody RegisterRequest request
    ) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(authService.register(request));
    }

    // POST /api/auth/avatar
    @PostMapping(value = "/avatar", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadRegisterAvatar(
            @Valid @RequestBody AvatarRequest request
    ) {
        String avatarUrl = authService.uploadAvatar(request);

        return ResponseEntity.ok(Map.of(
                "message", "Upload avatar successfully!",
                "avatarUrl", avatarUrl
        ));
    }

    // GET /api/auth/verify-email?token=...
    @GetMapping("/verify-email")
    @Operation(summary = "Verify email", security = {})
    public ResponseEntity<Void> verifyEmail(@RequestParam String token) {
        authService.verifyEmail(token);
        return ResponseEntity.noContent().build();   // 204 — redirect handled by client
    }

    // POST /api/auth/login
    @PostMapping("/login")
    @Operation(summary = "Login", security = {})
    public ResponseEntity<AuthResponse> login(
            @Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    // POST /api/auth/refresh
    @PostMapping("/refresh")
    public ResponseEntity<TokenPair> refresh(
            @Valid @RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    // POST /api/auth/logout
    // Requires: Authorization: Bearer <accessToken>
    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @AuthenticationPrincipal JwtPayload principal,
            @RequestBody(required = false) LogoutRequest request) {

        String deviceId = (request != null && request.deviceId() != null)
                ? request.deviceId()
                : principal.deviceId();

        authService.logout(principal.sub(), deviceId);
        return ResponseEntity.noContent().build();
    }

    // POST /api/auth/logout-all
    @PostMapping("/logout-all")
    public ResponseEntity<Void> logoutAll(
            @AuthenticationPrincipal JwtPayload principal) {
        authService.logoutAllDevices(principal.sub());
        return ResponseEntity.noContent().build();
    }
}
