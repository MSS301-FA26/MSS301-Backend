package com.cinemaai.identity.service;

import com.cinemaai.identity.client.CatalogClient;
import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.dto.request.auth.RegisterRequest;
import com.cinemaai.identity.entity.PendingRegistration;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.repository.PendingRegistrationRepository;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.security.JwtService;
import com.cinemaai.identity.service.impl.AuthServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AuthServiceRegistrationMappingTest {

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

    private AuthServiceImpl authService;

    @BeforeEach
    void setUp() {
        authService = new AuthServiceImpl(
                userRepository, userProfileRepository, pendingRegistrationRepository, passwordEncoder,
                authenticationManager, jwtService, jwtProperties, refreshTokenService,
                emailVerificationService, userRoleService, userService, userCinemaAssignmentService,
                googleTokenVerifier, catalogClient, mailService
        );
    }

    @Test
    void registerKeepsUsernameIdentityNumberAndPreferredCinemaUntilEmailVerification() {
        RegisterRequest request = new RegisterRequest(
                "customer@example.com", "Password@123", "Nguyen Van A", "0901234567", 2000,
                "nguyenvana", "012345678901", 12L
        );
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByIdentityNumber(anyString())).thenReturn(false);
        when(pendingRegistrationRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.findByIdentityNumber(anyString())).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.findByEmail(anyString())).thenReturn(Optional.empty());
        when(passwordEncoder.encode(request.password())).thenReturn("encoded-password");
        when(pendingRegistrationRepository.save(any(PendingRegistration.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        authService.register(request);

        ArgumentCaptor<PendingRegistration> pendingCaptor = ArgumentCaptor.forClass(PendingRegistration.class);
        verify(pendingRegistrationRepository).save(pendingCaptor.capture());
        PendingRegistration pending = pendingCaptor.getValue();
        assertEquals("nguyenvana", pending.getUsername());
        assertEquals("012345678901", pending.getIdentityNumber());
        assertEquals(12L, pending.getPreferredCinemaId());
        verify(catalogClient).validateActiveCinema(12L);
    }

    @Test
    void verifyEmailMapsRegistrationFieldsToTheUserWithoutExposingIdentityNumber() {
        PendingRegistration pending = new PendingRegistration(
                "customer@example.com", "encoded-password", "Nguyen Van A", "0901234567", 2000,
                "nguyenvana", "012345678901", 12L, "123456", LocalDateTime.now().plusMinutes(1)
        );
        when(pendingRegistrationRepository.findByEmail(anyString())).thenReturn(Optional.of(pending));
        when(pendingRegistrationRepository.findByUsername(anyString())).thenReturn(Optional.empty());
        when(pendingRegistrationRepository.findByIdentityNumber(anyString())).thenReturn(Optional.empty());
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userProfileRepository.existsByPhone("0901234567")).thenReturn(false);
        when(userRepository.existsByUsername(anyString())).thenReturn(false);
        when(userRepository.existsByIdentityNumber(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenAnswer(invocation -> invocation.getArgument(0));

        authService.verifyEmail("customer@example.com", "123456");

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(userCaptor.capture());
        User user = userCaptor.getValue();
        assertEquals("nguyenvana", user.getUsername());
        assertEquals("012345678901", user.getIdentityNumber());
        assertEquals(12L, user.getPreferredCinemaId());
        verify(userRoleService).assignRole(eq(user), eq(RoleName.CUSTOMER));
        verify(pendingRegistrationRepository).delete(pending);
    }
}
