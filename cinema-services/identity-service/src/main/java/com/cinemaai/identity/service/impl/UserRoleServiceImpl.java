package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.entity.Role;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserRole;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.repository.RoleRepository;
import com.cinemaai.identity.repository.UserRoleRepository;
import com.cinemaai.identity.service.UserRoleService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Override
    @Transactional
    public void assignRole(User user, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(new UserRole(user, role));
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<String> getRoleNames(Long userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("Role not found: " + roleName));
    }
}
