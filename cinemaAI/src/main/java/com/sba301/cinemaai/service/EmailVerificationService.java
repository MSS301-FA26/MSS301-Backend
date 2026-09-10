package com.sba301.cinemaai.service;

import com.sba301.cinemaai.entity.EmailVerificationToken;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.enums.EmailOtpPurpose;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.repository.EmailVerificationTokenRepository;
import com.sba301.cinemaai.repository.UserRepository;
import java.security.SecureRandom;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

public interface EmailVerificationService {

        public EmailVerificationToken create(User user);

        public EmailVerificationToken createGoogleLoginOtp(User user);

        public void resendVerificationOtp(String email);

        public void verifyEmail(String email, String otp);

        public User verifyGoogleLogin(String email, String otp);

        public void verify(String token);
}