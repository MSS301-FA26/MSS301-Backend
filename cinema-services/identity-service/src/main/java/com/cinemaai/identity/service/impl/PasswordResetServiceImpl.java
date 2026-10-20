package com.cinemaai.identity.service.impl;

import com.cinemaai.identity.entity.PasswordResetToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.exception.NotFoundException;
import com.cinemaai.identity.repository.PasswordResetTokenRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.MailService;
import com.cinemaai.identity.service.PasswordResetService;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordResetServiceImpl implements PasswordResetService {

    private static final int PASSWORD_RESET_OTP_EXPIRES_IN_MINUTES = 5;
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();

    private final UserRepository userRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final MailService mailService;

    @Override
    @Transactional
    public PasswordResetToken request(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new NotFoundException("User not found: " + email));
        PasswordResetToken resetToken = passwordResetTokenRepository.save(
                new PasswordResetToken(
                        user,
                        generateOtp(),
                        LocalDateTime.now().plusMinutes(PASSWORD_RESET_OTP_EXPIRES_IN_MINUTES)
                )
        );
        mailService.sendOtp(user.getEmail(), resetToken.getToken(), "Đặt lại mật khẩu");
        return resetToken;
    }

    @Override
    @Transactional
    public void confirm(String email, String otp, String newPassword, String confirmPassword) {
        if (!newPassword.equals(confirmPassword)) {
            throw new BadRequestException("Confirm password does not match");
        }

        PasswordResetToken resetToken = findValidResetToken(email, otp);
        resetToken.getUser().setPasswordHash(passwordEncoder.encode(newPassword));
        resetToken.setUsed(true);
    }

    @Override
    @Transactional(readOnly = true)
    public void verifyOtp(String email, String otp) {
        findValidResetToken(email, otp);
    }

    private PasswordResetToken findValidResetToken(String email, String otp) {
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findFirstByUserEmailAndTokenAndUsedFalseOrderByCreatedAtDesc(email, otp)
                .orElseThrow(() -> new BadRequestException("Invalid password reset OTP"));
        if (resetToken.isUsed() || resetToken.getExpiresAt().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Password reset OTP is expired or already used");
        }
        return resetToken;
    }

    private String generateOtp() {
        return String.format("%06d", SECURE_RANDOM.nextInt(1_000_000));
    }
}
