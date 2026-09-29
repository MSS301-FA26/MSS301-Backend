package com.cinemaai.booking.security;

import com.cinemaai.booking.client.IdentityClient;
import com.cinemaai.booking.client.dto.UserAccessScopeDto;
import com.cinemaai.booking.exception.ForbiddenException;
import com.cinemaai.booking.exception.UnauthorizedException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class CinemaSecurityService {

    private final IdentityClient identityClient;

    public UserAccessScopeDto getAuthoritativeScope(AuthenticatedUser user) {
        if (user == null || user.id() == null) {
            throw new UnauthorizedException("Yêu cầu xác thực tài khoản");
        }
        return identityClient.getUserAccessScope(user.id());
    }

    public void validateCinemaAccess(AuthenticatedUser user, Long targetCinemaId, boolean allowStaff) {
        UserAccessScopeDto scope = getAuthoritativeScope(user);

        boolean isAdmin = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
        if (isAdmin) {
            return;
        }

        boolean isManager = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("MANAGER") || r.equalsIgnoreCase("ROLE_MANAGER"));
        boolean isStaff = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("STAFF") || r.equalsIgnoreCase("ROLE_STAFF"));

        if (isStaff && !allowStaff) {
            throw new ForbiddenException("Nhân viên không có quyền thực hiện chức năng này");
        }

        if (!isManager && !isStaff) {
            throw new ForbiddenException("Bạn không có quyền hạn truy cập tài nguyên này");
        }

        Long userCinemaId = scope.cinemaId();
        if (userCinemaId == null) {
            throw new ForbiddenException("Tài khoản chưa được phân công rạp cụ thể");
        }

        if (targetCinemaId != null && !userCinemaId.equals(targetCinemaId)) {
            throw new ForbiddenException("Bạn không có quyền thao tác trên dữ liệu của rạp khác (Rạp được phân công: " + userCinemaId + ")");
        }
    }

    public Long resolveEnforcedCinemaId(AuthenticatedUser user, Long requestedCinemaId, boolean allowStaff) {
        UserAccessScopeDto scope = getAuthoritativeScope(user);

        boolean isAdmin = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("ADMIN") || r.equalsIgnoreCase("ROLE_ADMIN"));
        if (isAdmin) {
            return requestedCinemaId;
        }

        boolean isManager = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("MANAGER") || r.equalsIgnoreCase("ROLE_MANAGER"));
        boolean isStaff = scope.roles() != null && scope.roles().stream()
                .anyMatch(r -> r.equalsIgnoreCase("STAFF") || r.equalsIgnoreCase("ROLE_STAFF"));

        if (isStaff && !allowStaff) {
            throw new ForbiddenException("Nhân viên không có quyền truy cập chức năng này");
        }

        if (!isManager && !isStaff) {
            throw new ForbiddenException("Bạn không có quyền hạn truy cập tài nguyên này");
        }

        Long userCinemaId = scope.cinemaId();
        if (userCinemaId == null) {
            throw new ForbiddenException("Tài khoản chưa được phân công rạp cụ thể");
        }

        return userCinemaId;
    }
}
