package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.EmailVerificationToken;
import com.cinemaai.identity.enums.EmailOtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface EmailVerificationTokenRepository extends JpaRepository<EmailVerificationToken, Long> {

    Optional<EmailVerificationToken> findByToken(String token);

    Optional<EmailVerificationToken> findFirstByUserEmailAndTokenAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            String token,
            EmailOtpPurpose purpose
    );

    Optional<EmailVerificationToken> findFirstByUserEmailAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String email,
            EmailOtpPurpose purpose
    );
}
