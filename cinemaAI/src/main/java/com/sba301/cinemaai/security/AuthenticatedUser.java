package com.sba301.cinemaai.security;

import com.sba301.cinemaai.enums.UserStatus;
import java.util.Collection;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

public record AuthenticatedUser(
        Long id,
        String email,
        String password,
        UserStatus status,
        boolean emailVerified,
        boolean phoneVerified,
        Collection<? extends GrantedAuthority> authorities
) implements UserDetails {

    @Override
    public String getUsername() {
        return email;
    }

    @Override
    public String getPassword() {
        return password;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return authorities;
    }

    @Override
    public boolean isEnabled() {
        return status == UserStatus.ACTIVE && (emailVerified || phoneVerified);
    }
}
