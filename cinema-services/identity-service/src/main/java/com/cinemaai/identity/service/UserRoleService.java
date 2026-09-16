package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.Role;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import java.util.List;

public interface UserRoleService {

    void assignRole(User user, RoleName roleName);

    List<String> getRoleNames(Long userId);

    Role getRole(RoleName roleName);
}
