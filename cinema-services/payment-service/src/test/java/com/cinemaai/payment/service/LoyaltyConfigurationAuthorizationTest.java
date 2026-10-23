package com.cinemaai.payment.service;

import com.cinemaai.payment.service.impl.LoyaltyServiceImpl;
import com.cinemaai.payment.security.AuthenticatedUser;
import com.cinemaai.payment.exception.ForbiddenException;
import java.util.List;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.junit.jupiter.api.Test;
import static org.junit.jupiter.api.Assertions.*;

class LoyaltyConfigurationAuthorizationTest {
    @Test void missingActorAndUnverifiedManagerScopeFailClosed() {
        var service = new LoyaltyServiceImpl(null, null, null, null, null);
        assertThrows(ForbiddenException.class, () -> service.updateConfiguration(null));
        for (String role : List.of("USER", "CUSTOMER", "STAFF", "MANAGER")) {
            var actor = new AuthenticatedUser(7L, "test@test.com", List.of(new SimpleGrantedAuthority("ROLE_"+role)));
            assertThrows(ForbiddenException.class, () -> service.updateConfiguration(null, actor));
        }
    }
}
