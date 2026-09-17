package com.cinemaai.identity.seeder.implSeed;

import com.cinemaai.identity.config.SeederAccountProperties;
import com.cinemaai.identity.entity.Role;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserRole;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.repository.RoleRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.repository.UserRoleRepository;
import com.cinemaai.identity.seeder.Seeder;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(15)
@RequiredArgsConstructor
public class AdminAccountSeeder implements Seeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeederAccountProperties seederAccountProperties;

    @Override
    @Transactional
    public void seed() {
        Role adminRole = roleRepository.findByName(RoleName.ADMIN)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.ADMIN)));

        SeederAccountProperties.Account adminAccount = seederAccountProperties.getAdmin();
        if (adminAccount.getEmail() == null || adminAccount.getEmail().isBlank()) {
            return;
        }

        User admin = userRepository.findByEmail(adminAccount.getEmail())
                .orElseGet(() -> {
                    User user = new User(
                            adminAccount.getEmail(),
                            passwordEncoder.encode(adminAccount.getPassword() != null ? adminAccount.getPassword() : "Admin123"),
                            adminAccount.getFullName() != null ? adminAccount.getFullName() : "CinemaAI Admin",
                            adminAccount.getPhone() != null ? adminAccount.getPhone() : "0900000001"
                    );
                    user.setEmailVerified(true);
                    user.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(user);
                });

        if (!admin.isEmailVerified()) {
            admin.setEmailVerified(true);
            admin.setStatus(UserStatus.ACTIVE);
        }

        if (!userRoleRepository.existsByUserIdAndRoleId(admin.getId(), adminRole.getId())) {
            userRoleRepository.save(new UserRole(admin, adminRole));
        }
    }
}
