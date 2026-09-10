package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.PasswordResetToken;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordResetTokenRepository extends JpaRepository<PasswordResetToken, Long> {

    Optional<PasswordResetToken> findByToken(String token);

    Optional<PasswordResetToken> findFirstByUserEmailAndTokenAndUsedFalseOrderByCreatedAtDesc(
            String email,
            String token
    );
}
