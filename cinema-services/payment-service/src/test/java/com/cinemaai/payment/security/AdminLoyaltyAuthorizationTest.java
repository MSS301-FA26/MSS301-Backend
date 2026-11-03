package com.cinemaai.payment.security;

import com.cinemaai.payment.controller.AdminLoyaltyController;
import com.cinemaai.payment.service.LoyaltyService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.util.*;
import java.nio.charset.StandardCharsets;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.mockito.Mockito.verifyNoInteractions;

@WebMvcTest(AdminLoyaltyController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties={"APPLICATION_NAME=loyalty-auth-test", "server.address=127.0.0.1", "server.port=0", "app.gateway.secret=test", "app.internal.secret=test", "app.jwt.secret=01234567890123456789012345678901"})
class AdminLoyaltyAuthorizationTest {
    @Autowired MockMvc mvc;
    @MockBean LoyaltyService service;
    private String token(String role) {
        return Jwts.builder().subject("test").claim("userId",7L).claim("roles",List.of(role))
                .expiration(new Date(System.currentTimeMillis()+60000))
                .signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))).compact();
    }
    private org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[] writes() {
        return new org.springframework.test.web.servlet.request.MockHttpServletRequestBuilder[]{
            put("/api/v1/admin/loyalty/config").contentType("application/json").content("{\"earningRatePercent\":1,\"redemptionPoints\":1000,\"redemptionValueVnd\":1000,\"expiryMonth\":1,\"expiryDay\":1}"),
            post("/api/v1/admin/loyalty/add").contentType("application/json").content("{\"userId\":99,\"points\":100}"),
            post("/api/v1/admin/loyalty/99/redeem").param("points","10"),
            post("/api/v1/admin/loyalty/points/expire-all-active")};
    }
    @Test void writesDenyUnprivilegedActorsAndSpoofedHeaders() throws Exception {
        for(String role: List.of("USER","CUSTOMER","STAFF")) {
            for(var request:writes()) mvc.perform(request.header("X-Gateway-Secret","test")
                    .header("Authorization","Bearer "+token(role)).header("X-User-Roles","ADMIN"))
                    .andExpect(status().isForbidden());
        }
        verifyNoInteractions(service);
    }
    @Test void missingAndInvalidJwtCannotReadOrWrite() throws Exception {
        mvc.perform(get("/api/v1/admin/loyalty/config").header("X-Gateway-Secret","test")).andExpect(status().isUnauthorized());
        for(var request:writes()) mvc.perform(request.header("X-Gateway-Secret","test")
                .header("Authorization","Bearer invalid")).andExpect(status().isUnauthorized());
        verifyNoInteractions(service);
    }
    @Test void adminCanReachAllActionsWithCorrectTargets() throws Exception {
        for(String suffix:List.of("config","report","transactions")) mvc.perform(get("/api/v1/admin/loyalty/"+suffix)
                .header("X-Gateway-Secret","test").header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isOk());
        for(var request:writes()) mvc.perform(request.header("X-Gateway-Secret","test")
                .header("Authorization","Bearer "+token("ADMIN"))).andExpect(status().isOk());
        org.mockito.Mockito.verify(service).redeemPoints(99L,10);
        org.mockito.Mockito.verify(service).expireAllActivePoints("ADMIN");
        org.mockito.Mockito.verify(service).addPoints(org.mockito.ArgumentMatchers.argThat(r -> r.getUserId()==99L && r.getPoints()==100));
        org.mockito.Mockito.verify(service).updateConfiguration(org.mockito.ArgumentMatchers.any(),org.mockito.ArgumentMatchers.argThat(u -> u.id()==7L));
    }
    @Test void unprivilegedRolesCannotReadAdminData() throws Exception {
        for (String role : List.of("USER", "CUSTOMER", "STAFF")) {
            String jwt = Jwts.builder().subject("test").claim("userId", 7L).claim("roles", List.of(role))
                    .expiration(new Date(System.currentTimeMillis()+60000))
                    .signWith(Keys.hmacShaKeyFor("01234567890123456789012345678901".getBytes(StandardCharsets.UTF_8))).compact();
            for (String suffix : List.of("config", "report", "transactions")) {
                mvc.perform(get("/api/v1/admin/loyalty/"+suffix).header("X-Gateway-Secret", "test")
                        .header("Authorization", "Bearer "+jwt).header("X-User-Roles", "ADMIN"))
                        .andExpect(status().isForbidden());
            }
        }
        verifyNoInteractions(service);
    }
}
