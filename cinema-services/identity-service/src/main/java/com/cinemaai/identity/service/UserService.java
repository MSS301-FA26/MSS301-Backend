package com.cinemaai.identity.service;

import com.cinemaai.identity.dto.request.user.AdminManagerCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminStaffCreateRequest;
import com.cinemaai.identity.dto.request.user.AdminUserStatusUpdateRequest;
import com.cinemaai.identity.dto.request.user.ChangePasswordRequest;
import com.cinemaai.identity.dto.request.user.UserProfileUpdateRequest;
import com.cinemaai.identity.dto.response.user.UserAccessScopeResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import java.util.List;

public interface UserService {

    User getByEmail(String email);

    UserProfileResponse getProfile(String email);

    UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request);

    UserProfileResponse updateAvatar(String email, String avatarUrl);

    void changePassword(String email, ChangePasswordRequest request);

    List<UserProfileResponse> getAllUsers(RoleName role);

    default List<UserProfileResponse> getAllUsers() {
        return getAllUsers(null);
    }

    List<UserProfileResponse> getAllUsersForActor(RoleName role, String actorEmail);

    UserProfileResponse getById(Long id);

    UserProfileResponse getByIdForActor(Long id, String actorEmail);

    UserProfileResponse createManager(AdminManagerCreateRequest request, String actorEmail);

    UserProfileResponse createStaff(AdminStaffCreateRequest request);

    UserProfileResponse createStaffForActor(AdminStaffCreateRequest request, String actorEmail);

    UserProfileResponse assignCinema(Long userId, Long cinemaId, String actorEmail);

    UserProfileResponse updateStatus(Long id, AdminUserStatusUpdateRequest request);

    UserProfileResponse updateStatusForActor(Long id, AdminUserStatusUpdateRequest request, String actorEmail);

    UserAccessScopeResponse getAccessScope(Long userId);

    UserProfileResponse toProfile(User user);
}
