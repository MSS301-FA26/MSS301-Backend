package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.config.JwtProperties;
import com.cinemaai.identity.dto.request.auth.GoogleLoginRequest;
import com.cinemaai.identity.dto.request.auth.GoogleOtpVerifyRequest;
import com.cinemaai.identity.dto.request.auth.LoginRequest;
import com.cinemaai.identity.dto.request.auth.RegisterRequest;
import com.cinemaai.identity.dto.response.auth.AuthResponse;
import com.cinemaai.identity.dto.response.auth.RegisterResponse;
import com.cinemaai.identity.dto.response.user.UserProfileResponse;
import com.cinemaai.identity.entity.PendingRegistration;
import com.cinemaai.identity.entity.RefreshToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.RoleName;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.ConflictException;
import com.cinemaai.identity.exception.UnauthorizedException;
import com.cinemaai.identity.repository.PendingRegistrationRepository;
import com.cinemaai.identity.repository.UserProfileRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.security.JwtService;
import com.cinemaai.identity.service.AuthService;
import com.cinemaai.identity.service.EmailVerificationService;
import com.cinemaai.identity.service.GoogleTokenVerifier;
import com.cinemaai.identity.service.MailService;
import com.cinemaai.identity.service.RefreshTokenService;
import com.cinemaai.identity.service.UserRoleService;
import com.cinemaai.identity.service.UserService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final int EMAIL_VERIFICATION_EXPIRES_IN_SECONDS = 90;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final UserProfileRepository userProfileRepository;
    private final PendingRegistrationRepository pendingRegistrationRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final RefreshTokenService refreshTokenService;
    private final EmailVerificationService emailVerificationService;
    private final UserRoleService userRoleService;
    private final UserService userService;
    private final GoogleTokenVerifier googleTokenVerifier;
    private final MailService mailService;

    @Override
    @Transactional
    public RegisterResponse register(RegisterRequest request) {
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        if (request.phone() != null && !request.phone().isBlank()) {
            if (userProfileRepository.existsByPhone(request.phone())) {
                throw new ConflictException("Phone already exists");
            }
            pendingRegistrationRepository.findByPhone(request.phone())
                    .filter(pendingRegistration -> !pendingRegistration.getEmail().equals(request.email()))
                    .ifPresent(pendingRegistration -> {
                        throw new ConflictException("Phone already exists");
                    });
        }

        String otp = generateOtp();
        LocalDateTime expiresAt = LocalDateTime.now().plusSeconds(EMAIL_VERIFICATION_EXPIRES_IN_SECONDS);
        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(request.email())
                .map(existing -> {
                    refreshPendingRegistration(
                            existing,
                            passwordEncoder.encode(request.password()),
                            request.fullName(),
                            request.phone(),
                            request.birthYear(),
                            otp,
                            expiresAt
                    );
                    return existing;
                })
                .orElseGet(() -> pendingRegistrationRepository.save(new PendingRegistration(
                        request.email(),
                        passwordEncoder.encode(request.password()),
                        request.fullName(),
                        request.phone(),
                        request.birthYear(),
                        otp,
                        expiresAt
                )));
        mailService.sendOtp(pendingRegistration.getEmail(), pendingRegistration.getOtp(), "Email verification");
        return new RegisterResponse(
                toPendingProfile(pendingRegistration),
                true,
                EMAIL_VERIFICATION_EXPIRES_IN_SECONDS
        );
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        PendingRegistration pendingRegistration = pendingRegistrationRepository.findByEmail(email).orElse(null);
        if (pendingRegistration == null) {
            emailVerificationService.resendVerificationOtp(email);
            return;
        }

        refreshPendingRegistration(
                pendingRegistration,
                pendingRegistration.getPasswordHash(),
                pendingRegistration.getFullName(),
                pendingRegistration.getPhone(),
                pendingRegistration.getBirthYear(),
                generateOtp(),
                LocalDateTime.now().plusSeconds(EMAIL_VERIFICATION_EXPIRES_IN_SECONDS)
        );
        mailService.sendOtp(pendingRegistration.getEmail(), pendingRegistration.getOtp(), "Email verification");
    }

    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        PendingRegistration pendingRegistration = findPendingRegistration(email, otp);
        if (pendingRegistration == null) {
            emailVerificationService.verifyEmail(email, otp);
            return;
        }
        if (pendingRegistration.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP is expired or already used");
        }
        if (userRepository.existsByEmail(pendingRegistration.getEmail())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new ConflictException("Email already exists");
        }
        if (pendingRegistration.getPhone() != null && !pendingRegistration.getPhone().isBlank()
                && userProfileRepository.existsByPhone(pendingRegistration.getPhone())) {
            pendingRegistrationRepository.delete(pendingRegistration);
            throw new ConflictException("Phone already exists");
        }

        User user = userRepository.save(new User(
                pendingRegistration.getEmail(),
                pendingRegistration.getPasswordHash(),
                pendingRegistration.getFullName(),
                pendingRegistration.getPhone(),
                pendingRegistration.getBirthYear()
        ));
        activateEmail(user);
        userRoleService.assignRole(user, RoleName.CUSTOMER);
        pendingRegistrationRepository.delete(pendingRegistration);
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        try {
            authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(request.username(), request.password())
            );
        } catch (DisabledException exception) {
            throw new UnauthorizedException("User is disabled or email is not verified");
        } catch (BadCredentialsException exception) {
            throw new UnauthorizedException("Invalid username or password");
        }

        User user = userService.getByEmail(request.username());
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogle(GoogleLoginRequest request) {
        GoogleTokenVerifier.GoogleTokenInfo tokenInfo = googleTokenVerifier.verify(request.credential());
        User user = userRepository.findByEmail(tokenInfo.email())
                .orElseGet(() -> createGoogleUser(tokenInfo));

        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("User is disabled");
        }
        if (!user.isEmailVerified()) {
            activateEmail(user);
        }

        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse loginWithGoogleOtp(GoogleOtpVerifyRequest request) {
        User user = emailVerificationService.verifyGoogleLogin(request.email(), request.otp());
        if (user.getStatus() == UserStatus.DISABLED) {
            throw new UnauthorizedException("User is disabled");
        }
        return createAuthResponse(user);
    }

    @Override
    @Transactional
    public AuthResponse refresh(String refreshTokenValue) {
        RefreshToken refreshToken = refreshTokenService.validate(refreshTokenValue);
        return createAuthResponse(refreshToken.getUser());
    }

    @Override
    @Transactional
    public void logout(String refreshTokenValue) {
        refreshTokenService.revoke(refreshTokenValue);
    }

    private User createGoogleUser(GoogleTokenVerifier.GoogleTokenInfo tokenInfo) {
        String fullName = tokenInfo.name();
        if (fullName == null || fullName.isBlank()) {
            fullName = tokenInfo.email();
        }

        User user = userRepository.save(new User(
                tokenInfo.email(),
                passwordEncoder.encode(tokenInfo.subject()),
                fullName,
                null
        ));
        activateEmail(user);
        userRoleService.assignRole(user, RoleName.CUSTOMER);
        return user;
    }

    private void refreshPendingRegistration(
            PendingRegistration pendingRegistration,
            String passwordHash,
            String fullName,
            String phone,
            Integer birthYear,
            String otp,
            LocalDateTime expiresAt
    ) {
        pendingRegistration.setPasswordHash(passwordHash);
        pendingRegistration.setFullName(fullName);
        pendingRegistration.setPhone(phone);
        pendingRegistration.setBirthYear(birthYear);
        pendingRegistration.setOtp(otp);
        pendingRegistration.setExpiresAt(expiresAt);
    }

    private void activateEmail(User user) {
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }

    private AuthResponse createAuthResponse(User user) {
        List<String> roles = userRoleService.getRoleNames(user.getId());
        RefreshToken refreshToken = refreshTokenService.create(user);
        return new AuthResponse(
                jwtService.generateAccessToken(user.getId(), user.getEmail(), roles),
                refreshToken.getToken(),
                "Bearer",
                jwtProperties.accessExpirationMs(),
                userService.toProfile(user),
                roles
        );
    }

    private PendingRegistration findPendingRegistration(String email, String otp) {
        if (email == null || email.isBlank()) {
            return pendingRegistrationRepository.findFirstByOtpOrderByCreatedAtDesc(otp).orElse(null);
        }
        return pendingRegistrationRepository.findByEmail(email)
                .filter(pendingRegistration -> pendingRegistration.getOtp().equals(otp))
                .orElse(null);
    }

    private UserProfileResponse toPendingProfile(PendingRegistration pendingRegistration) {
        return new UserProfileResponse(
                null,
                pendingRegistration.getEmail(),
                pendingRegistration.getFullName(),
                pendingRegistration.getPhone(),
                null,
                pendingRegistration.getBirthYear(),
                UserStatus.PENDING_VERIFICATION,
                false,
                false,
                List.of(RoleName.CUSTOMER.name()),
                pendingRegistration.getCreatedAt(),
                pendingRegistration.getUpdatedAt()
        );
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
