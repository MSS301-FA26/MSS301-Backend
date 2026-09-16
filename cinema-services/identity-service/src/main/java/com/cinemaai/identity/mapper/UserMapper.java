package com.cinemaai.identity.mapper;

import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.entity.UserProfile;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class UserMapper {

    public UserProfileResponse toProfile(User user, List<String> roles) {
        UserProfile profile = user.getProfile();
        if (profile == null) {
            return new UserProfileResponse(
                    user.getId(),
                    user.getEmail(),
                    null,
                    null,
                    null,
                    user.getBirthYear(),
                    user.getStatus(),
                    user.isEmailVerified(),
                    false,
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
