package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.PendingRegistration;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PendingRegistrationRepository extends JpaRepository<PendingRegistration, Long> {

    Optional<PendingRegistration> findByEmail(String email);

    Optional<PendingRegistration> findByPhone(String phone);

    Optional<PendingRegistration> findFirstByOtpOrderByCreatedAtDesc(String otp);
}
