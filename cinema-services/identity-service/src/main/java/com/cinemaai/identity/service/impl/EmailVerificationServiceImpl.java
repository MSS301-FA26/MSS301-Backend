package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.entity.EmailVerificationToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.EmailOtpPurpose;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.repository.EmailVerificationTokenRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.EmailVerificationService;
import com.cinemaai.identity.service.MailService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int EMAIL_VERIFICATION_EXPIRES_IN_SECONDS = 90;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

    @Override
    @Transactional
    public EmailVerificationToken create(User user) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.save(
                new EmailVerificationToken(
                        user,
                        generateOtp(),
                        EmailOtpPurpose.EMAIL_VERIFICATION,
                        LocalDateTime.now().plusSeconds(EMAIL_VERIFICATION_EXPIRES_IN_SECONDS)
                )
        );
        mailService.sendOtp(user.getEmail(), verificationToken.getToken(), "Email verification");
        return verificationToken;
    }

    @Override
    @Transactional
    public EmailVerificationToken createGoogleLoginOtp(User user) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.save(
                new EmailVerificationToken(
                        user,
                        generateOtp(),
                        EmailOtpPurpose.GOOGLE_LOGIN,
                        LocalDateTime.now().plusMinutes(5)
                )
        );
        mailService.sendOtp(user.getEmail(), verificationToken.getToken(), "Google login");
        return verificationToken;
    }

    @Override
    @Transactional
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        create(user);
    }

    @Override
    @Transactional
    public void verifyEmail(String email, String otp) {
        EmailVerificationToken verificationToken = findValidOtp(email, otp, EmailOtpPurpose.EMAIL_VERIFICATION);
        activateEmail(verificationToken.getUser());
        verificationToken.setUsed(true);
    }

    @Override
    @Transactional
    public User verifyGoogleLogin(String email, String otp) {
        EmailVerificationToken verificationToken = findValidOtp(email, otp, EmailOtpPurpose.GOOGLE_LOGIN);
        activateEmail(verificationToken.getUser());
        verificationToken.setUsed(true);
        return verificationToken.getUser();
    }

    @Override
    @Transactional
    public void verify(String token) {
        EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(token)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));
        if (verificationToken.isUsed() || verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP is expired or already used");
        }
        activateEmail(verificationToken.getUser());
        verificationToken.setUsed(true);
    }

    private EmailVerificationToken findValidOtp(String email, String otp, EmailOtpPurpose purpose) {
        if (email == null || email.isBlank()) {
            EmailVerificationToken verificationToken = emailVerificationTokenRepository.findByToken(otp)
                    .filter(token -> token.getPurpose() == purpose)
                    .filter(token -> !token.isUsed())
                    .orElseThrow(() -> new BadRequestException("Invalid OTP"));
            if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
                throw new BadRequestException("OTP is expired or already used");
            }
            return verificationToken;
        }

        EmailVerificationToken verificationToken = emailVerificationTokenRepository
                .findFirstByUserEmailAndTokenAndPurposeAndUsedFalseOrderByCreatedAtDesc(email, otp, purpose)
                .orElseThrow(() -> new BadRequestException("Invalid OTP"));
        if (verificationToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("OTP is expired or already used");
        }
        return verificationToken;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }

    private void activateEmail(User user) {
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }
}
