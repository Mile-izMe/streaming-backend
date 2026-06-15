package com.melody.melody_stream.modules.auth.service;

import com.melody.melody_stream.core.enums.UserStatus;
import com.melody.melody_stream.core.exception.AuthException;
import com.melody.melody_stream.infrastructure.minio.service.MinioWriteService;
import com.melody.melody_stream.modules.auth.dto.request.AvatarRequest;
import com.melody.melody_stream.modules.auth.dto.request.LoginRequest;
import com.melody.melody_stream.modules.auth.dto.request.RefreshTokenRequest;
import com.melody.melody_stream.modules.auth.dto.request.RegisterRequest;
import com.melody.melody_stream.modules.auth.dto.response.AuthResponse;
import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import com.melody.melody_stream.modules.auth.dto.response.RegisterResponse;
import com.melody.melody_stream.modules.auth.dto.response.TokenPair;
import com.melody.melody_stream.modules.auth.entity.User;
import com.melody.melody_stream.modules.auth.event.UserRegisteredEvent;
import com.melody.melody_stream.modules.auth.repository.UserRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtTokenService jwtTokenService;
    private final RefreshTokenService refreshTokenService;
    private final MinioWriteService minioWriteService;
    
    private final ApplicationEventPublisher applicationEventPublisher;

    // ── Register ──────────────────────────────────────────────

    @Transactional
    public RegisterResponse register(RegisterRequest request) {

        // 1. Check duplicates
        if (userRepository.existsByEmail(request.email())) {
            throw new AuthException("Email already in use.");
        }
        if (userRepository.existsByUsername(request.username())) {
            throw new AuthException("Username already taken.");
        }

        // 2. Generate verification token (UUID is sufficient; no need for JWT here)
        String verificationToken = UUID.randomUUID().toString();
        LocalDateTime tokenExpiry = LocalDateTime.now().plusHours(24);

        // 3. Build and save user (status = PENDING until email verified)
        User user = User.builder()
                .username(request.username())
                .email(request.email())
                .password(passwordEncoder.encode(request.password()))
                .status(UserStatus.PENDING)
                .verificationToken(verificationToken)
                .verificationTokenExpiry(tokenExpiry)
                .build();

        userRepository.save(user);

        // 4. Fire-and-forget mail (async - won't fail the transaction)
        // Because the transaction can only execute in database range
        // mailService.sendVerificationEmail(user.getEmail(), user.getUsername(), verificationToken);

        // WHEN and ONLY WHEN Database has commited successfully
        applicationEventPublisher.publishEvent(new UserRegisteredEvent(user.getEmail(), user.getUsername(), verificationToken));

        log.info("User registered: username={}, email={}", user.getUsername(), user.getEmail());

        return new RegisterResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                "Verification email sent. Please check your inbox."
        );
    }

    // ── Upload Avatar ──────────────────────────────────────────────
    @Transactional
    public String uploadAvatar(AvatarRequest request) {
        // 1. Validate file
        if (request.file().isEmpty()) {
            throw new IllegalArgumentException("File must not be empty");
        }

        // Accepted image only
        String contentType = request.file().getContentType();
        if (contentType == null || !contentType.startsWith("image/")) {
            throw new IllegalArgumentException("Only accepted image format");
        }

        // 2. Get user from DB
        User user = userRepository.findById(request.userId())
                .orElseThrow(() -> new RuntimeException("User Not Found"));

        // 3. Create Key for image & upload to MinIO
        String fileKey = "avatars/user-" + user.getId() + "-" + UUID.randomUUID().toString() + ".jpg";

        try {
            minioWriteService.uploadBuffer(fileKey, request.file().getBytes(), contentType);
        } catch (Exception e) {
            log.error("[AuthService] Failed to upload avatar for users: {}", user.getUsername(), e);
            throw new RuntimeException("Can not upload image, please try again later");
        }

        user.setAvatarUrl(fileKey);
        userRepository.save(user);

        return fileKey;
    }

    // ── Verify email ──────────────────────────────────────────

    @Transactional
    public void verifyEmail(String token) {
        User user = userRepository.findByVerificationToken(token)
                .orElseThrow(() -> new AuthException("Invalid or expired verification link."));

        if (user.getVerificationTokenExpiry().isBefore(LocalDateTime.now())) {
            throw new AuthException("Verification link has expired. Please request a new one");
        }

        if (user.isActive()) {
            throw new AuthException("Email already verified");
        }

        user.setStatus(UserStatus.ACTIVE);
        user.setVerificationToken(null);
        user.setVerificationTokenExpiry(null);
        userRepository.save(user);

        log.info("Email verified for userId={}", user.getId());
    }

    // ── Login ─────────────────────────────────────────────────

    @Transactional
    public AuthResponse login(LoginRequest request) {
        // 1. Load user with roles + permissions (single query)
        User user = userRepository.findByEmailWithRolesAndPermissions(request.email())
                .orElseThrow(() -> new AuthException("Invalid email or password."));

        // 2. Verify password
        if (!passwordEncoder.matches(request.password(), user.getPassword())) {
            throw new AuthException("Invalid email or password.");
        }

        // 3. Check account status
        switch (user.getStatus()) {
            case PENDING -> throw new AuthException(
                    "Please verify your email before logging in."
            );
            case BANNED -> throw new AuthException(
                    "Your account has been suspended. Please contact support."
            );
            case ACTIVE -> {} // proceed
        }

        // 4. Resolve deviceId - generate a stable one if client didn't send
        String deviceId = request.deviceId() != null
                ? request.deviceId()
                : UUID.randomUUID().toString();

        // 5. Build JWT payload
        JwtPayload payload = buildPayload(user, deviceId);

        // 6. Generate token pair
        String accessToken = jwtTokenService.generateAccessToken(payload);
        String refreshToken = jwtTokenService.generateRefreshToken(payload);

        // 7. Persist refresh token (Redis + DB)
        refreshTokenService.storeToken(
                user.getId(), deviceId, refreshToken,
                jwtTokenService.getRefreshTtl()
        );

        log.info("User logged in: userId={}, deviceId={}", user.getId(), deviceId);

        return new AuthResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                payload.roles(),
                new TokenPair(accessToken, refreshToken)
        );
    }

    // ── Refresh token rotation ────────────────────────────────

    @Transactional
    public TokenPair refreshToken(RefreshTokenRequest request) {
        String incomingToken = request.refreshToken();

        // 1. Decode WITHOUT verifying signature - sub + deviceId
        JwtPayload unsafePayload = jwtTokenService.decodeRefreshTokenUnsafe(incomingToken);
        String userId = unsafePayload.sub();
        String deviceId = unsafePayload.deviceId() != null
                ? unsafePayload.deviceId()
                : request.deviceId();

        // 2. Compare against Redis - detects reuse before expensive DB Query
        refreshTokenService.validateAgainstRedis(userId, deviceId, incomingToken);

        // 3. Fully verify JWT signature + expiry
        jwtTokenService.parseRefreshToken(incomingToken);

        // 4. Load freshest user data (roles may have changed since last login)
        User user = userRepository.findByIdWithRolesAndPermissions(userId)
                .orElseThrow(() -> new AuthException("User not found"));

        if (!user.isActive()) {
            throw new AuthException("Account is not active.");
        }

        // 5. Generate new token pair
        JwtPayload payload      = buildPayload(user, deviceId);
        String newAccessToken   = jwtTokenService.generateAccessToken(payload);
        String newRefreshToken  = jwtTokenService.generateRefreshToken(payload);

        // 6. Rotate — overwrite Redis + create new DB record
        refreshTokenService.storeToken(
                userId, deviceId, newRefreshToken,
                jwtTokenService.getRefreshTtl()
        );

        log.info("Token rotated: userId={} deviceId={}", userId, deviceId);

        return new TokenPair(newAccessToken, newRefreshToken);
    }

    // ── Logout ────────────────────────────────────────────────

    @Transactional
    public void logout(String userId, String deviceId) {
        refreshTokenService.revokeDevice(userId, deviceId);
        log.info("Logout: userId={} deviceId={}", userId, deviceId);
    }

    @Transactional
    public void logoutAllDevices(String userId) {
        refreshTokenService.revokeAllDevices(userId);
        log.info("Logout all devices: userId={}", userId);
    }

    // ── Private helpers ───────────────────────────────────────

    private JwtPayload buildPayload(User user, String deviceId) {
        List<String> roles = user.getRoles().stream()
                .map(ur -> ur.getRole().getName().name())
                .toList();

        List<String> permissions = user.getRoles().stream()
                .flatMap(ur -> ur.getRole().getPermissions().stream())
                .map(rp -> rp.getPermission().getName())
                .distinct()
                .toList();

        return new JwtPayload(
                user.getId(),
                user.getUsername(),
                roles,
                permissions,
                deviceId
        );
    }
}
