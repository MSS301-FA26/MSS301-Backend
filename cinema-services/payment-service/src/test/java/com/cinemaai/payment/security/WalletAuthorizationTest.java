package com.cinemaai.payment.security;

import com.cinemaai.payment.controller.WalletController;
import com.cinemaai.payment.service.WalletService;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoMoreInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WalletController.class)
@Import(SecurityConfig.class)
@TestPropertySource(properties = {
        "APPLICATION_NAME=wallet-security-test",
        "server.address=127.0.0.1",
        "server.port=0",
        "app.gateway.secret=gateway-test",
        "app.internal.secret=internal-test",
        "app.jwt.secret=01234567890123456789012345678901"
})
class WalletAuthorizationTest {
    private static final String KEY = "01234567890123456789012345678901";

    @Autowired MockMvc mvc;
    @MockBean WalletService walletService;

    @Test
    void walletReadsAlwaysUseAuthenticatedUserInsteadOfSpoofedIdentity() throws Exception {
        var headers = headers(7L, "USER");
        headers.set("X-User-Id", "99");
        headers.set("X-User-Roles", "ADMIN");

        mvc.perform(get("/api/v1/wallet").headers(headers)).andExpect(status().isOk());
        mvc.perform(get("/api/v1/wallet/transactions").headers(headers).param("page", "0").param("size", "20"))
                .andExpect(status().isOk());
        mvc.perform(get("/api/v1/wallet/withdrawals").headers(headers).param("page", "0").param("size", "20"))
                .andExpect(status().isOk());

        verify(walletService).getWallet(7L);
        verify(walletService).getTransactions(7L, 0, 20);
        verify(walletService).getMyWithdrawals(7L, 0, 20);
        verifyNoMoreInteractions(walletService);
    }

    @Test
    void withdrawalUsesAuthenticatedIdentityForUserAndAdmin() throws Exception {
        String body = """
                {"amount":10000,"bankName":"Test Bank","accountNumber":"1234","accountHolder":"Test User","walletPhone":"0900","userId":99}
                """;

        mvc.perform(post("/api/v1/wallet/withdrawals").headers(headers(7L, "USER"))
                        .header("X-User-Id", "99").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());
        mvc.perform(post("/api/v1/wallet/withdrawals").headers(headers(8L, "ADMIN"))
                        .header("X-User-Id", "99").contentType(MediaType.APPLICATION_JSON).content(body))
                .andExpect(status().isOk());

        verify(walletService).createWithdrawalRequest(org.mockito.ArgumentMatchers.eq(7L), any());
        verify(walletService).createWithdrawalRequest(org.mockito.ArgumentMatchers.eq(8L), any());
    }

    @Test
    void walletEndpointsRequireValidJwt() throws Exception {
        mvc.perform(get("/api/v1/wallet").header("X-Gateway-Secret", "gateway-test"))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/v1/wallet/transactions")
                        .header("X-Gateway-Secret", "gateway-test")
                        .header("Authorization", "Bearer invalid"))
                .andExpect(status().isUnauthorized());
    }

    private org.springframework.http.HttpHeaders headers(Long userId, String role) {
        var headers = new org.springframework.http.HttpHeaders();
        headers.set("X-Gateway-Secret", "gateway-test");
        headers.setBearerAuth(Jwts.builder().subject("user@test.com")
                .claim("userId", userId)
                .claim("roles", List.of(role))
                .expiration(new Date(System.currentTimeMillis() + 60_000))
                .signWith(Keys.hmacShaKeyFor(KEY.getBytes(StandardCharsets.UTF_8)))
                .compact());
        return headers;
    }
}
