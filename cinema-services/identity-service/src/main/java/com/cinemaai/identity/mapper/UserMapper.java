package com.cinemaai.identity.mapper;

import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.User;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class UserMapper {

    public UserProfileResponse toProfile(User user, List<String> roles) {
        return toProfile(user, roles, null);
    }

    @Mapping(target = "id", source = "user.id")
    @Mapping(target = "email", source = "user.email")
    @Mapping(target = "username", source = "user.username")
    @Mapping(target = "fullName", source = "user.profile.fullName")
    @Mapping(target = "phone", source = "user.profile.phone")
    @Mapping(target = "avatarUrl", source = "user.profile.avatarUrl")
    @Mapping(target = "birthYear", source = "user.birthYear")
    @Mapping(target = "preferredCinemaId", source = "user.preferredCinemaId")
    @Mapping(target = "status", source = "user.status")
    @Mapping(target = "emailVerified", source = "user.emailVerified")
    @Mapping(target = "phoneVerified", expression = "java(user.getProfile() != null && user.getProfile().isPhoneVerified())")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "cinemaId", source = "cinemaId")
    @Mapping(target = "createdAt", expression = "java(user.getProfile() != null && user.getProfile().getCreatedAt() != null ? user.getProfile().getCreatedAt() : user.getCreatedAt())")
    @Mapping(target = "updatedAt", expression = "java(user.getProfile() != null && user.getProfile().getUpdatedAt() != null ? user.getProfile().getUpdatedAt() : user.getUpdatedAt())")
    public abstract UserProfileResponse toProfile(User user, List<String> roles, Long cinemaId);
}
