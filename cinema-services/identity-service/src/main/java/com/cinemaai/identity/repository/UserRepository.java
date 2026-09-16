package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);
}
