package com.melody.melody_stream.modules.auth.service;

import com.melody.melody_stream.modules.auth.dto.response.JwtPayload;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.SignatureAlgorithm;
import io.jsonwebtoken.security.Keys;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Component
@Slf4j
@Getter
public class JwtTokenService {

    private final SecretKey accessSecret;
    private final SecretKey refreshSecret;
    private final Duration accessTtl;
    private final Duration refreshTtl;

    public JwtTokenService(
            @Value("${jwt.access-secret}")
            String accessSecret,

            @Value("${jwt.refresh-secret}")
            String refreshSecret,

            @Value("${jwt.access-ttl-ms}")
            long accessTtl,

            @Value("${jwt.refresh-ttl-ms}")
            long refreshTtl
    ) {
        this.accessSecret = Keys.hmacShaKeyFor(accessSecret.getBytes(StandardCharsets.UTF_8));
        this.refreshSecret = Keys.hmacShaKeyFor(refreshSecret.getBytes(StandardCharsets.UTF_8));
        this.accessTtl = Duration.ofMillis(accessTtl);
        this.refreshTtl = Duration.ofMillis(refreshTtl);
    }

    // ── Generate ──────────────────────────────────────────────

    public String generateAccessToken(JwtPayload payload) {
        return buildToken(payload, accessSecret, accessTtl);
    }

     public String generateRefreshToken(JwtPayload payload) {
         // RT payload needs sub + deviceId to minimize exposure
         return Jwts.builder()
                 .setId(UUID.randomUUID().toString())
                 .setSubject(payload.sub())
                 .claim("deviceId", payload.deviceId())
                 .setIssuedAt(new Date())
                 .setExpiration(new Date(System.currentTimeMillis() + refreshTtl.toMillis()))
                 .signWith(refreshSecret, SignatureAlgorithm.HS256)
                 .compact();
     }

    // ── Parse ─────────────────────────────────────────────────

    /**
     * Parse and validate access token — throws on invalid/expired.
     */
    public JwtPayload parseAccessToken(String token) {
        Claims claims = parseClaims(token, accessSecret);
        return claimsToPayload(claims);
    }

    /**
     * Decode refresh token without signature verification.
     * Used to extract sub + deviceId BEFORE Redis lookup.
     * Signature is verified separately after Redis validation.
     */
    @SuppressWarnings("unchecked")
    public JwtPayload decodeRefreshTokenUnsafe(String token) {
        // Split and decode the payload part only (index 1)
        String[] parts   = token.split("\\.");
        byte[]   decoded = java.util.Base64.getUrlDecoder().decode(parts[1]);
        try {
            var node     = new com.fasterxml.jackson.databind.ObjectMapper()
                    .readTree(decoded);
            String sub      = node.get("sub").asText();
            String deviceId = node.has("deviceId") ? node.get("deviceId").asText() : null;
            return new JwtPayload(sub, null, null, null, deviceId);
        } catch (Exception e) {
            throw new JwtException("Cannot decode refresh token payload");
        }
    }

    /**
     * Fully verify refresh token signature + expiry after Redis check.
     */
    public JwtPayload parseRefreshToken(String token) {
        Claims claims = parseClaims(token, refreshSecret);
        String deviceId = claims.get("deviceId", String.class);
        return new JwtPayload(claims.getSubject(), null, null, null, deviceId);
    }

    // ── Private helpers ───────────────────────────────────────

    private String buildToken(JwtPayload payload, SecretKey key, Duration ttl) {
        return Jwts.builder()
                .setId(UUID.randomUUID().toString())
                .setSubject(payload.sub())
                .claim("username",    payload.username())
                .claim("roles",       payload.roles())
                .claim("permissions", payload.permissions())
                .claim("deviceId",    payload.deviceId())
                .setIssuedAt(new Date())
                .setExpiration(new Date(System.currentTimeMillis() + ttl.toMillis()))
                .signWith(key, SignatureAlgorithm.HS256)
                .compact();
    }

    private Claims parseClaims(String token, SecretKey key) {
        return Jwts.parserBuilder()
                .setSigningKey(key)
                .build()
                .parseClaimsJws(token)
                .getBody();
    }

    @SuppressWarnings("unchecked")
    private JwtPayload claimsToPayload(Claims claims) {
        return new JwtPayload(
                claims.getSubject(),
                claims.get("username",    String.class),
                claims.get("roles",       List.class),
                claims.get("permissions", List.class),
                claims.get("deviceId",    String.class)
        );
    }
}
