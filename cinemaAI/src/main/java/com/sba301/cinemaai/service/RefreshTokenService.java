package com.sba301.cinemaai.service;

import com.sba301.cinemaai.entity.RefreshToken;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.exception.UnauthorizedException;
import com.sba301.cinemaai.repository.RefreshTokenRepository;
import com.sba301.cinemaai.security.JwtProperties;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface RefreshTokenService {

        public RefreshToken create(User user);

        public RefreshToken validate(String token);

        public void revoke(String token);

        public void revokeAll(User user);
}