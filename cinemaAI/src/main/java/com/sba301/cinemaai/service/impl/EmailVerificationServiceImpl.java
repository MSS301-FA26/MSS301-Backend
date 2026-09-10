package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.entity.EmailVerificationToken;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.EmailOtpPurpose;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.EmailVerificationTokenRepository;
import com.sba301.cinemaai.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import com.sba301.cinemaai.service.EmailVerificationService;
import com.sba301.cinemaai.service.MailService;

@Service
@RequiredArgsConstructor
public class EmailVerificationServiceImpl implements EmailVerificationService {

    private static final int EMAIL_VERIFICATION_EXPIRES_IN_SECONDS = 90;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final EmailVerificationTokenRepository emailVerificationTokenRepository;
    private final UserRepository userRepository;
    private final MailService mailService;

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

    @Transactional
    public void resendVerificationOtp(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found"));
        if (user.isEmailVerified()) {
            throw new BadRequestException("Email is already verified");
        }
        create(user);
    }

    @Transactional
    public void verifyEmail(String email, String otp) {
        EmailVerificationToken verificationToken = findValidOtp(email, otp, EmailOtpPurpose.EMAIL_VERIFICATION);
        activateEmail(verificationToken.getUser());
        verificationToken.setUsed(true);
    }

    @Transactional
    public User verifyGoogleLogin(String email, String otp) {
        EmailVerificationToken verificationToken = findValidOtp(email, otp, EmailOtpPurpose.GOOGLE_LOGIN);
        activateEmail(verificationToken.getUser());
        verificationToken.setUsed(true);
        return verificationToken.getUser();
    }

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
