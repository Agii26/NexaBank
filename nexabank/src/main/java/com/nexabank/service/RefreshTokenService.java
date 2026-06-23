package com.nexabank.service;

import com.nexabank.exception.TokenException;
import com.nexabank.model.RefreshToken;
import com.nexabank.model.User;
import com.nexabank.repository.RefreshTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;

@Service
@RequiredArgsConstructor
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${app.jwt.refresh-expiration-ms}")
    private long refreshExpirationMs;

    /** Generate a secure random token, hash it, persist it, return the raw value. */
    @Transactional
    public String createRefreshToken(User user) {
        // Revoke all existing tokens for this user (single-session model)
        refreshTokenRepository.revokeAllByUserId(user.getId());

        String rawToken = generateSecureToken();

        RefreshToken token = RefreshToken.builder()
                .user(user)
                .tokenHash(hashToken(rawToken))
                .expiresAt(LocalDateTime.now().plusSeconds(refreshExpirationMs / 1000))
                .build();

        refreshTokenRepository.save(token);
        return rawToken;
    }

    /** Validate the raw token and return the associated User. */
    @Transactional
    public User validateAndRotate(String rawToken, String newAccessTokenEmail) {
        RefreshToken stored = refreshTokenRepository.findByTokenHash(hashToken(rawToken))
                .orElseThrow(() -> new TokenException("Refresh token not found or already used"));

        if (stored.isRevoked()) {
            throw new TokenException("Refresh token has been revoked");
        }
        if (stored.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new TokenException("Refresh token has expired — please log in again");
        }

        return stored.getUser();
    }

    @Transactional
    public void revokeAllForUser(Long userId) {
        refreshTokenRepository.revokeAllByUserId(userId);
    }

    // ── Private helpers ────────────────────────────────────────────────────────

    private String generateSecureToken() {
        byte[] bytes = new byte[48];
        new SecureRandom().nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    /** Simple SHA-256 style hash using available Java stdlib — no extra dep needed. */
    private String hashToken(String token) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            return Base64.getEncoder().encodeToString(hash);
        } catch (java.security.NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not available", e);
        }
    }
}
