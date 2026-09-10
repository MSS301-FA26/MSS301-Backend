package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.repository.RoleRepository;
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
