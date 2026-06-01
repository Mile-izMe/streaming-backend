package com.melody.melody_stream.modules.auth.service;

import com.melody.melody_stream.core.exception.AuthException;
import com.melody.melody_stream.modules.auth.entity.RefreshToken;
import com.melody.melody_stream.modules.auth.repository.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final RedisTemplate<String, Object> redisTemplate;
    private static final String REDIS_KEY_PATTERN = "rt:%s:%s"; // rt:{userId}:{deviceId}

    // ── Store ─────────────────────────────────────────────────

    /**
     * Store raw token in Redis (session) + hashed token in DB (audit/revoke).
     * Value serialized as JSON via GenericJacksonJsonRedisSerializer.
     */
    @Transactional
    public void storeToken(String userId, String deviceId, String rawToken, Duration ttl) {
        String redisKey = redisKey(userId, deviceId);
        String tokenHash = sha256(rawToken);

        // Overwrite Redis session - GenericJackSonJsonRedis Serializer handles String fine
        redisTemplate.opsForValue().set(redisKey, rawToken, ttl.toSeconds(), TimeUnit.SECONDS);

        // Persist hash to DB
        RefreshToken record = RefreshToken.builder()
                .tokenHash(tokenHash)
                .userId(userId)
                .deviceId(deviceId)
                .expiresAt(LocalDateTime.now().plus(ttl))
                .isRevoked(false)
                .build();

        refreshTokenRepository.save(record);
    }

    // ── Validate ──────────────────────────────────────────────

    /**
     * Compare incoming token against Redis.
     * - null     → session expired or logged out
     * - mismatch → reuse attack
     */
    public void validateAgainstRedis(String userId, String deviceId, String incomingToken) {
        String redisKey = redisKey(userId, deviceId);
        Object stored = redisTemplate.opsForValue().get(redisKey);

        if (stored == null) {
            throw new AuthException("Session expired. Please login again");
        }

        // GenericJacksonJsonRedisSerializer deserializes String back as String
        String storedToken = stored.toString();

        if (!storedToken.equals(incomingToken)) {
            log.warn("Refresh token reuse detected - userId={} deviceId={}", userId, deviceId);
            handleReuseAttack(userId, deviceId);
            throw new AuthException("Token reuse detected. All sessions have been invalidated.");
        }
    }

    // ── Revoke single device ──────────────────────────────────

    @Transactional
    public void revokeDevice(String userId, String deviceId) {
        redisTemplate.delete(redisKey(userId, deviceId));

        refreshTokenRepository
                .findByUserIdAndDeviceIdAndIsRevokedFalse(userId, deviceId)
                .ifPresent(token -> {
                    token.revoke("system");
                    refreshTokenRepository.save(token);
                });
    }

    // ── Revoke all devices ────────────────────────────────────

    @Transactional
    public void revokeAllDevices(String userId) {
        List<RefreshToken> activeTokens =
                refreshTokenRepository.findByUserIdAndIsRevokedFalse(userId);

        // Delete each Redis key by deviceId
        activeTokens.stream()
                .filter(t -> t.getDeviceId() != null)
                .map(t -> redisKey(userId, t.getDeviceId()))
                .forEach(redisTemplate::delete);

        // Bulk revoke in DB
        refreshTokenRepository.revokeAllByUserId(userId, LocalDateTime.now(), "system");
    }

    // ── Reuse attack ──────────────────────────────────────────

    @Transactional
    public void handleReuseAttack(String userId, String deviceId) {
        // Immediately kill the suspect session
        redisTemplate.delete(redisKey(userId, deviceId));

        // Revoke all sessions — force full re-login
        refreshTokenRepository.revokeAllByUserId(
                userId, LocalDateTime.now(), "reuse-detection"
        );
    }

    // ── Helpers ───────────────────────────────────────────────

    private String redisKey(String userId, String deviceId) {
        return REDIS_KEY_PATTERN.formatted(userId, deviceId);
    }

    public static String sha256(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
