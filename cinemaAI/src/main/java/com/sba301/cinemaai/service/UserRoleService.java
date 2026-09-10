package com.sba301.cinemaai.service;

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

public interface UserRoleService {

        public void assignRole(User user, RoleName roleName);

        public List<String> getRoleNames(Long userId);

        public Role getRole(RoleName roleName);
}