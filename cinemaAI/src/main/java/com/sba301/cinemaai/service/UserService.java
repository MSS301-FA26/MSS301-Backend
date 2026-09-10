package com.sba301.cinemaai.service;

import com.sba301.cinemaai.dto.request.user.AdminUserStatusUpdateRequest;
import com.sba301.cinemaai.dto.request.user.AdminStaffCreateRequest;
import com.sba301.cinemaai.dto.request.user.ChangePasswordRequest;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.dto.request.user.UserProfileUpdateRequest;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.UserMapper;
import com.sba301.cinemaai.repository.UserProfileRepository;
import com.sba301.cinemaai.repository.UserRepository;
import java.util.List;

public interface UserService {

        public User getByEmail(String email);

        public UserProfileResponse getProfile(String email);

        public UserProfileResponse updateProfile(String email, UserProfileUpdateRequest request);

        public UserProfileResponse updateAvatar(String email, String avatarUrl);

        public void changePassword(String email, ChangePasswordRequest request);

        public List<UserProfileResponse> getAllUsers();

        public UserProfileResponse getById(Long id);

        public UserProfileResponse createStaff(AdminStaffCreateRequest request);

        public UserProfileResponse updateStatus(Long id, AdminUserStatusUpdateRequest request);

        public UserProfileResponse toProfile(User user);
}
