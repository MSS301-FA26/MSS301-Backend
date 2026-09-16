package com.cinemaai.identity.seeder;

import com.cinemaai.identity.config.SeederAccountProperties;
import com.cinemaai.identity.entity.Role;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserRole;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.repository.RoleRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(16)
@RequiredArgsConstructor
public class StaffAccountSeeder implements Seeder {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final PasswordEncoder passwordEncoder;
    private final SeederAccountProperties seederAccountProperties;

    @Override
    @Transactional
    public void seed() {
        Role staffRole = roleRepository.findByName(RoleName.STAFF)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.STAFF)));

        SeederAccountProperties.Account staffAccount = seederAccountProperties.getStaff();
        if (staffAccount.getEmail() == null || staffAccount.getEmail().isBlank()) {
            return;
        }

        User staff = userRepository.findByEmail(staffAccount.getEmail())
                .orElseGet(() -> {
                    User user = new User(
                            staffAccount.getEmail(),
                            passwordEncoder.encode(staffAccount.getPassword() != null ? staffAccount.getPassword() : "Staff123"),
                            staffAccount.getFullName() != null ? staffAccount.getFullName() : "CinemaAI Staff",
                            staffAccount.getPhone() != null ? staffAccount.getPhone() : "0900000002"
                    );
                    user.setEmailVerified(true);
                    user.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(user);
                });

        if (!staff.isEmailVerified()) {
            staff.setEmailVerified(true);
            staff.setStatus(UserStatus.ACTIVE);
        }

        if (!userRoleRepository.existsByUserIdAndRoleId(staff.getId(), staffRole.getId())) {
            userRoleRepository.save(new UserRole(staff, staffRole));
        }
    }
}
