package com.sba301.cinemaai.auth;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.EmailVerificationRequest;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.auth.LogoutRequest;
import com.sba301.cinemaai.dto.request.auth.PasswordResetConfirmRequest;
import com.sba301.cinemaai.dto.request.auth.PasswordResetRequest;
import com.sba301.cinemaai.dto.request.auth.RefreshTokenRequest;
import com.sba301.cinemaai.dto.request.auth.RegisterRequest;
import com.sba301.cinemaai.dto.request.user.ChangePasswordRequest;
import com.sba301.cinemaai.dto.request.user.UserProfileUpdateRequest;
import com.sba301.cinemaai.repository.PasswordResetTokenRepository;
import com.sba301.cinemaai.repository.PendingRegistrationRepository;
import com.sba301.cinemaai.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AuthIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private PendingRegistrationRepository pendingRegistrationRepository;

    @Autowired
    private PasswordResetTokenRepository passwordResetTokenRepository;

    @Autowired
    private UserRepository userRepository;

    @Test
    void shouldRegisterVerifyLoginRefreshAndLogout() throws Exception {
        String email = "phase2.customer@example.com";

        String registerBody = objectMapper.writeValueAsString(new RegisterRequest(
                email,
                "Password123",
                "Phase Two Customer",
                "0900111222",
                2000
        ));

        mockMvc.perform(post("/api/v1/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(registerBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.user.email").value(email))
                .andExpect(jsonPath("$.data.user.roles[0]").value("CUSTOMER"))
                .andExpect(jsonPath("$.data.user.birthYear").value(2000))
                .andExpect(jsonPath("$.data.emailVerificationRequired").value(true));

        org.assertj.core.api.Assertions.assertThat(userRepository.existsByEmail(email)).isFalse();

        String verificationToken = pendingRegistrationRepository
                .findByEmail(email)
                .orElseThrow()
                .getOtp();

        mockMvc.perform(post("/api/v1/auth/verify-email")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new EmailVerificationRequest(verificationToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        org.assertj.core.api.Assertions.assertThat(userRepository.existsByEmail(email)).isTrue();
        org.assertj.core.api.Assertions.assertThat(pendingRegistrationRepository.findByEmail(email)).isEmpty();

        String loginResponse = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.data.user.emailVerified").value(true))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode loginJson = objectMapper.readTree(loginResponse);
        String accessToken = loginJson.at("/data/accessToken").asText();
        String refreshToken = loginJson.at("/data/refreshToken").asText();

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.email").value(email))
                .andExpect(jsonPath("$.data.birthYear").value(2000));

        mockMvc.perform(put("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new UserProfileUpdateRequest(
                                "Phase Two Customer",
                                "0900111222",
                                1998
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birthYear").value(1998));

        mockMvc.perform(get("/api/v1/users/me")
                        .header("Authorization", "Bearer " + accessToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.birthYear").value(1998));

        mockMvc.perform(post("/api/v1/users/me/password")
                        .header("Authorization", "Bearer " + accessToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new ChangePasswordRequest(
                                "Password123",
                                "NewPassword456",
                                "NewPassword456"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "Password123"))))
                .andExpect(status().isUnauthorized());

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "NewPassword456"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/password-reset/request")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetRequest(email))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.token").isNotEmpty());

        String passwordResetOtp = passwordResetTokenRepository
                .findFirstByUserEmailAndTokenAndUsedFalseOrderByCreatedAtDesc(
                        email,
                        passwordResetTokenRepository.findAll().get(0).getToken()
                )
                .orElseThrow()
                .getToken();

        mockMvc.perform(post("/api/v1/auth/password-reset/confirm")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new PasswordResetConfirmRequest(
                                email,
                                passwordResetOtp,
                                "ResetPassword789",
                                "ResetPassword789"
                        ))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(email, "ResetPassword789"))))
                .andExpect(status().isOk());

        mockMvc.perform(post("/api/v1/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new RefreshTokenRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty());

        mockMvc.perform(post("/api/v1/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LogoutRequest(refreshToken))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }
}
