package com.cinemaai.catalog.security;

import com.cinemaai.catalog.exception.ForbiddenException;
import com.cinemaai.catalog.exception.UnauthorizedException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class CinemaSecurityService {

    public boolean isAdmin(AuthenticatedUser user) {
        return user != null && user.isAdmin();
    }

    public Long getAssignedCinemaId(AuthenticatedUser user) {
        return user != null ? user.cinemaId() : null;
    }

    public void validateCinemaAccess(AuthenticatedUser user, Long targetCinemaId) {
        if (user == null) {
            throw new UnauthorizedException("Yêu cầu xác thực tài khoản");
        }

        if (user.isAdmin()) {
            return;
        }

        if (user.isManager()) {
            Long userCinemaId = user.cinemaId();
            if (userCinemaId == null) {
                throw new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp cụ thể");
            }
            if (targetCinemaId != null && !userCinemaId.equals(targetCinemaId)) {
                throw new ForbiddenException("Quản lý không có quyền thao tác trên dữ liệu của rạp khác (Rạp được phân công: " + userCinemaId + ")");
            }
            return;
        }

        throw new ForbiddenException("Bạn không có quyền hạn truy cập tài nguyên này");
    }

    public Long resolveEnforcedCinemaId(AuthenticatedUser user, Long requestedCinemaId) {
        if (user == null) {
            return requestedCinemaId;
        }

        if (user.isAdmin()) {
            return requestedCinemaId;
        }

        if (user.isManager()) {
            Long userCinemaId = user.cinemaId();
            if (userCinemaId == null) {
                throw new ForbiddenException("Tài khoản Quản lý chưa được phân công cụm rạp cụ thể");
            }
            if (requestedCinemaId != null && !userCinemaId.equals(requestedCinemaId)) {
                throw new ForbiddenException("Quản lý không có quyền truy cập dữ liệu của rạp khác (Rạp được phân công: " + userCinemaId + ")");
            }
            return userCinemaId;
        }

        return requestedCinemaId;
    }
}
