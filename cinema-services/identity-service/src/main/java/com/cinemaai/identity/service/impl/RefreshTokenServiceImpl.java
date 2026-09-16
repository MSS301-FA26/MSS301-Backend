package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.exception.UnauthorizedException;
import com.cinemaai.identity.repository.RefreshTokenRepository;
import com.cinemaai.identity.service.RefreshTokenService;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class RefreshTokenServiceImpl implements RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final JwtProperties jwtProperties;

    @Override
    @Transactional
    public RefreshToken create(User user) {
        LocalDateTime expiresAt = LocalDateTime.now().plusNanos(jwtProperties.refreshExpirationMs() * 1_000_000);
        return refreshTokenRepository.save(new RefreshToken(user, UUID.randomUUID().toString(), expiresAt));
    }

    @Override
    @Transactional(readOnly = true)
    public RefreshToken validate(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new UnauthorizedException("Invalid refresh token"));
        if (refreshToken.isRevoked() || refreshToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new UnauthorizedException("Refresh token is expired or revoked");
        }
        return refreshToken;
    }

    @Override
    @Transactional
    public void revoke(String token) {
        refreshTokenRepository.findByToken(token).ifPresent(refreshToken -> {
            refreshToken.setRevoked(true);
            refreshTokenRepository.save(refreshToken);
        });
    }

    @Override
    @Transactional
    public void revokeAll(User user) {
        refreshTokenRepository.findByUserAndRevokedFalse(user)
                .forEach(refreshToken -> refreshToken.setRevoked(true));
    }
}
