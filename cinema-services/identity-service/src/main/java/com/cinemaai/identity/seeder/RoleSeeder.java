package com.cinemaai.identity.seeder;

import com.cinemaai.identity.entity.Role;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(10)
@RequiredArgsConstructor
public class RoleSeeder implements Seeder {

    private final RoleRepository roleRepository;

    @Override
    @Transactional
    public void seed() {
        for (RoleName roleName : RoleName.values()) {
            if (!roleRepository.existsByName(roleName)) {
                roleRepository.save(new Role(roleName));
            }
        }
    }
}
