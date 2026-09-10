package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.RefreshToken;
import com.sba301.cinemaai.entity.User;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    List<RefreshToken> findByUserAndRevokedFalse(User user);
}
