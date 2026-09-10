package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfile(User user, List<String> roles) {
        UserProfile profile = user.getProfile();
        if (profile == null) {
            // Handle case where user doesn't have a profile yet
            return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    null,  // fullName
                    null,  // phone
                    null,  // avatarUrl
                    user.getBirthYear(),
                    user.getStatus(),
                    user.isEmailVerified(),
                    false, // phoneVerified
                    roles,
                    user.getCreatedAt(),
                    user.getUpdatedAt()
            );
        }
        return new UserProfileResponse(
                user.getId(),
                user.getEmail(),
                profile.getFullName(),
                profile.getPhone(),
                profile.getAvatarUrl(),
                user.getBirthYear(),
                user.getStatus(),
                user.isEmailVerified(),
                profile.isPhoneVerified(),
                roles,
                profile.getCreatedAt(),
                profile.getUpdatedAt()
        );
    }
}
