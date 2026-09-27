package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.RoleName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    boolean existsByEmail(String email);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT u FROM User u JOIN UserRole ur ON ur.user.id = u.id WHERE ur.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") RoleName roleName);
}
