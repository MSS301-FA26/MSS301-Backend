package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.PhoneVerificationToken;
import com.sba301.cinemaai.enums.OtpPurpose;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhoneVerificationTokenRepository extends JpaRepository<PhoneVerificationToken, Long> {

    Optional<PhoneVerificationToken> findFirstByPhoneAndOtpAndPurposeAndUsedFalseOrderByCreatedAtDesc(
            String phone,
            String otp,
            OtpPurpose purpose
    );
}
