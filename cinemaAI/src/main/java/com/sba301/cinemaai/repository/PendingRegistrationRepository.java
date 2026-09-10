package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.PendingRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByEmail(String email);

    Optional<PendingRegistration> findByPhone(String phone);

    Optional<PendingRegistration> findFirstByOtpOrderByCreatedAtDesc(String otp);

}
