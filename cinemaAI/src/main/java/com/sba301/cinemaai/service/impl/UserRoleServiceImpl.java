package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sba301.cinemaai.service.UserRoleService;

@Service
@RequiredArgsConstructor
public class UserRoleServiceImpl implements UserRoleService {

    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;

    @Transactional
    public void assignRole(User user, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(new UserRole(user, role));
        }
    }

    @Transactional(readOnly = true)
    public List<String> getRoleNames(Long userId) {
        return userRoleRepository.findByUserId(userId)
                .stream()
                .map(userRole -> userRole.getRole().getName().name())
                .toList();
    }

    @Transactional(readOnly = true)
    public Role getRole(RoleName roleName) {
        return roleRepository.findByName(roleName)
                .orElseThrow(() -> new NotFoundException("Role not found"));
    }
}
