package com.cinemaai.identity.repository;

import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface UserRepository extends JpaRepository<User, Long> {

    Optional<User> findByEmail(String email);

    Optional<User> findByEmailOrUsername(String email, String username);

    boolean existsByEmail(String email);

    boolean existsByUsername(String username);

    boolean existsByIdentityNumber(String identityNumber);

    long countByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    @Query("SELECT u FROM User u JOIN UserRole ur ON ur.user.id = u.id WHERE ur.role.name = :roleName")
    List<User> findByRoleName(@Param("roleName") RoleName roleName);
}
