package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.EmailVerificationToken;
import com.sba301.cinemaai.enums.EmailOtpPurpose;
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
