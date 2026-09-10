package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.enums.RoleName;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoleRepository extends JpaRepository<Role, Long> {

    Optional<Role> findByName(RoleName name);

    boolean existsByName(RoleName name);
}
