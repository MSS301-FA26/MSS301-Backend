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
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@Order(16)
@RequiredArgsConstructor
public class ManagerAccountSeeder implements Seeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeederAccountProperties seederAccountProperties;

    @Override
    @Transactional
    public void seed() {
        Role managerRole = roleRepository.findByName(RoleName.MANAGER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.MANAGER)));

        SeederAccountProperties.Account managerAccount = seederAccountProperties.getManager();
        if (managerAccount.getEmail() == null || managerAccount.getEmail().isBlank()) {
            return;
        }

        User manager = userRepository.findByEmail(managerAccount.getEmail())
                .orElseGet(() -> {
                    User user = new User(
                            managerAccount.getEmail(),
                            passwordEncoder.encode(managerAccount.getPassword() != null ? managerAccount.getPassword() : "Admin123"),
                            managerAccount.getFullName() != null ? managerAccount.getFullName() : "CinemaAI Manager",
                            managerAccount.getPhone() != null ? managerAccount.getPhone() : "0900000003"
                    );
                    user.setEmailVerified(true);
                    user.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(user);
                });

        if (!manager.isEmailVerified()) {
            manager.setEmailVerified(true);
            manager.setStatus(UserStatus.ACTIVE);
        }

        if (!userRoleRepository.existsByUserIdAndRoleId(manager.getId(), managerRole.getId())) {
            userRoleRepository.save(new UserRole(manager, managerRole));
        }
        log.info("Seeded manager account: {}", manager.getEmail());
    }
}
