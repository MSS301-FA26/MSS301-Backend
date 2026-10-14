package com.cinemaai.identity.service;

import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.dto.request.auth.GoogleLoginRequest;
import com.cinemaai.identity.dto.request.auth.LoginRequest;
import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.entity.PendingRegistration;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.UnauthorizedException;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.repository.PendingRegistrationRepository;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.security.JwtService;
import com.cinemaai.identity.service.impl.AuthServiceImpl;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceCustomerActivationTest {
    @Mock private UserRepository userRepository;
    @Mock private UserProfileRepository userProfileRepository;
    @Mock private PendingRegistrationRepository pendingRegistrationRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private JwtProperties jwtProperties;
    @Mock private RefreshTokenService refreshTokenService;
    @Mock private EmailVerificationService emailVerificationService;
    @Mock private UserRoleService userRoleService;
    @Mock private UserService userService;
    @Mock private UserCinemaAssignmentService userCinemaAssignmentService;
    @Mock private GoogleTokenVerifier googleTokenVerifier;
    @Mock private CatalogClient catalogClient;
    @Mock private MailService mailService;
    @Mock private CustomerActivationOutboxService customerActivationOutboxService;
    @InjectMocks private AuthServiceImpl authService;

    @Test
    void normalOtpActivationEmitsOnceAndUsedOtpReplayDoesNotEmitAgain() {
        PendingRegistration pending = new PendingRegistration(
                "normal-customer@example.com", "encoded-password", "Customer", null, null,
                null, null, null, "test-otp", LocalDateTime.now().plusMinutes(1));
        when(pendingRegistrationRepository.findByEmail(pending.getEmail()))
                .thenReturn(Optional.of(pending)).thenReturn(Optional.empty());
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 715L);
            return saved;
        });
        doThrow(new BadRequestException("Invalid OTP"))
                .when(emailVerificationService).verifyEmail(pending.getEmail(), pending.getOtp());

        authService.verifyEmail(pending.getEmail(), pending.getOtp());
        assertThrows(BadRequestException.class,
                () -> authService.verifyEmail(pending.getEmail(), pending.getOtp()));

        ArgumentCaptor<User> activated = ArgumentCaptor.forClass(User.class);
        verify(customerActivationOutboxService).recordActivation(activated.capture());
        assertEquals(715L, activated.getValue().getId());
        assertTrue(activated.getValue().isEmailVerified());
        assertEquals(UserStatus.ACTIVE, activated.getValue().getStatus());
        verify(userRepository).save(any(User.class));
        verifyNoMoreInteractions(customerActivationOutboxService);
    }

    @Test
    void expiredPendingOtpDoesNotCreateCustomerOrProvisioningEvent() {
        PendingRegistration pending = new PendingRegistration(
                "normal-customer@example.com", "encoded-password", "Customer", null, null,
                null, null, null, "test-otp", LocalDateTime.now().minusMinutes(1));
        when(pendingRegistrationRepository.findByEmail(pending.getEmail())).thenReturn(Optional.of(pending));

        assertThrows(BadRequestException.class,
                () -> authService.verifyEmail(pending.getEmail(), pending.getOtp()));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(customerActivationOutboxService);
    }

    @Test
    void firstGoogleCustomerCreatesOneActivationWithInternalUserIdAfterCustomerRole() {
        GoogleTokenVerifier.GoogleTokenInfo token = googleToken();
        when(googleTokenVerifier.verify("google-test-credential")).thenReturn(token);
        when(userRepository.findByEmail(token.email())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(token.subject())).thenReturn("encoded-google-secret");
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> {
            User saved = invocation.getArgument(0);
            ReflectionTestUtils.setField(saved, "id", 812L);
            return saved;
        });
        stubAuthResponse(812L);

        authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential"));

        ArgumentCaptor<User> activated = ArgumentCaptor.forClass(User.class);
        verify(customerActivationOutboxService).recordActivation(activated.capture());
        User customer = activated.getValue();
        assertEquals(812L, customer.getId());
        assertTrue(customer.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        var order = inOrder(userRoleService, customerActivationOutboxService);
        order.verify(userRoleService).assignRole(customer, RoleName.CUSTOMER);
        order.verify(customerActivationOutboxService).recordActivation(customer);
        verifyNoMoreInteractions(customerActivationOutboxService);
    }

    @Test
    void returningActiveGoogleCustomerLogsInRepeatedlyWithoutProvisioningSideEffects() {
        User customer = user(812L, true, UserStatus.ACTIVE);
        when(googleTokenVerifier.verify("google-test-credential")).thenReturn(googleToken());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        stubAuthResponse(812L);

        authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential"));
        authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential"));

        verify(userRepository, never()).save(any(User.class));
        verifyNoInteractions(customerActivationOutboxService);
    }

    @Test
    void existingUnverifiedGoogleCustomerPublishesOnlyWhenItBecomesActive() {
        User customer = user(812L, false, UserStatus.PENDING_VERIFICATION);
        when(googleTokenVerifier.verify("google-test-credential")).thenReturn(googleToken());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));
        stubAuthResponse(812L);

        authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential"));
        authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential"));

        assertTrue(customer.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        verify(customerActivationOutboxService).recordActivation(customer);
        verifyNoMoreInteractions(customerActivationOutboxService);
    }

    @Test
    void disabledGoogleAccountIsRejectedWithoutProvisioning() {
        User customer = user(812L, true, UserStatus.DISABLED);
        when(googleTokenVerifier.verify("google-test-credential")).thenReturn(googleToken());
        when(userRepository.findByEmail(customer.getEmail())).thenReturn(Optional.of(customer));

        assertThrows(UnauthorizedException.class,
                () -> authService.loginWithGoogle(new GoogleLoginRequest("google-test-credential")));

        verifyNoInteractions(customerActivationOutboxService);
        assertEquals(UserStatus.DISABLED, customer.getStatus());
    }

    @Test
    void normalPasswordLoginDoesNotProvisionExistingCustomer() {
        User customer = user(812L, true, UserStatus.ACTIVE);
        when(userRepository.findByEmailOrUsername(customer.getEmail(), customer.getEmail()))
                .thenReturn(Optional.of(customer));
        stubAuthResponse(812L);

        authService.login(new LoginRequest(customer.getEmail(), "test-password"));

        verifyNoInteractions(customerActivationOutboxService);
        verify(userRepository, never()).save(any(User.class));
    }

    private void stubAuthResponse(Long id) {
        when(userRoleService.getRoleNames(id)).thenReturn(List.of("CUSTOMER"));
        when(refreshTokenService.create(any(User.class))).thenAnswer(invocation ->
                new RefreshToken(invocation.getArgument(0), "test-refresh-token", LocalDateTime.now().plusDays(1)));
        when(userCinemaAssignmentService.getCinemaIdByUserId(id)).thenReturn(Optional.empty());
    }

    private static User user(Long id, boolean verified, UserStatus status) {
        User user = new User("google-customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", id);
        user.setEmailVerified(verified);
        user.setStatus(status);
        return user;
    }

    private static GoogleTokenVerifier.GoogleTokenInfo googleToken() {
        return new GoogleTokenVerifier.GoogleTokenInfo(
                "external-google-subject", "test-audience", "google-customer@example.com", true,
                "Customer", null, null, null);
    }
}
