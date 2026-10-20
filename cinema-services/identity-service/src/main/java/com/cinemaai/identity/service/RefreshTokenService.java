package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.entity.User;

public interface RefreshTokenService {

    RefreshToken create(User user);

    RefreshToken validate(String token);

    void revoke(String token);

    void revokeAll(User user);
}
