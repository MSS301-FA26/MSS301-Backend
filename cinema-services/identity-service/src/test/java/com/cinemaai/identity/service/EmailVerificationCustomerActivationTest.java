package com.cinemaai.identity.service;

import com.cinemaai.identity.entity.EmailVerificationToken;
import com.cinemaai.identity.entity.User;
import com.cinemaai.identity.enums.EmailOtpPurpose;
import com.cinemaai.identity.enums.UserStatus;
import com.cinemaai.identity.exception.BadRequestException;
import com.cinemaai.identity.repository.EmailVerificationTokenRepository;
import com.cinemaai.identity.repository.UserRepository;
import com.cinemaai.identity.service.impl.EmailVerificationServiceImpl;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailVerificationCustomerActivationTest {
    @Mock private EmailVerificationTokenRepository emailVerificationTokenRepository;
    @Mock private UserRepository userRepository;
    @Mock private MailService mailService;
    @Mock private CustomerActivationOutboxService customerActivationOutboxService;
    @InjectMocks private EmailVerificationServiceImpl emailVerificationService;

    @Test
    void legacyEmailOtpActivationRecordsExistingInternalUserAfterBecomingActive() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.EMAIL_VERIFICATION);
        when(emailVerificationTokenRepository
                .findFirstByUserEmailAndTokenAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        customer.getEmail(), token.getToken(), EmailOtpPurpose.EMAIL_VERIFICATION))
                .thenReturn(Optional.of(token));

        emailVerificationService.verifyEmail(customer.getEmail(), token.getToken());

        assertTrue(customer.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        assertTrue(token.isUsed());
        verify(customerActivationOutboxService).recordActivation(customer);
    }

    @Test
    void legacyGoogleOtpActivationRecordsCustomerTransition() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.GOOGLE_LOGIN);
        when(emailVerificationTokenRepository
                .findFirstByUserEmailAndTokenAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        customer.getEmail(), token.getToken(), EmailOtpPurpose.GOOGLE_LOGIN))
                .thenReturn(Optional.of(token));

        User result = emailVerificationService.verifyGoogleLogin(customer.getEmail(), token.getToken());

        assertSame(customer, result);
        assertEquals(913L, result.getId());
        assertTrue(result.isEmailVerified());
        assertEquals(UserStatus.ACTIVE, result.getStatus());
        verify(customerActivationOutboxService).recordActivation(customer);
    }

    @Test
    void genericOtpActivationRecordsTransitionButRepeatedUsedTokenDoesNot() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.EMAIL_VERIFICATION);
        when(emailVerificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        emailVerificationService.verify(token.getToken());
        assertThrows(BadRequestException.class, () -> emailVerificationService.verify(token.getToken()));

        verify(customerActivationOutboxService).recordActivation(customer);
        verifyNoMoreInteractions(customerActivationOutboxService);
    }

    @Test
    void googleOtpLoginForAlreadyActiveUserDoesNotRecordActivation() {
        User customer = user(true, UserStatus.ACTIVE);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.GOOGLE_LOGIN);
        when(emailVerificationTokenRepository
                .findFirstByUserEmailAndTokenAndPurposeAndUsedFalseOrderByCreatedAtDesc(
                        customer.getEmail(), token.getToken(), EmailOtpPurpose.GOOGLE_LOGIN))
                .thenReturn(Optional.of(token));

        emailVerificationService.verifyGoogleLogin(customer.getEmail(), token.getToken());

        assertTrue(token.isUsed());
        verifyNoInteractions(customerActivationOutboxService);
    }

    @Test
    void legacyVerifiedUserStillRecordsTheTransitionFromPendingStatus() {
        User customer = user(true, UserStatus.PENDING_VERIFICATION);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.EMAIL_VERIFICATION);
        when(emailVerificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        emailVerificationService.verify(token.getToken());

        assertEquals(UserStatus.ACTIVE, customer.getStatus());
        verify(customerActivationOutboxService).recordActivation(customer);
    }

    @Test
    void legacyActiveUserStillRecordsTheTransitionFromUnverifiedEmail() {
        User customer = user(false, UserStatus.ACTIVE);
        EmailVerificationToken token = token(customer, EmailOtpPurpose.EMAIL_VERIFICATION);
        when(emailVerificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        emailVerificationService.verify(token.getToken());

        assertTrue(customer.isEmailVerified());
        verify(customerActivationOutboxService).recordActivation(customer);
    }

    @Test
    void expiredOtpNeverActivatesOrRecordsProvisioning() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        EmailVerificationToken token = new EmailVerificationToken(
                customer, "expired-test-otp", LocalDateTime.now().minusMinutes(1));
        when(emailVerificationTokenRepository.findByToken(token.getToken())).thenReturn(Optional.of(token));

        assertThrows(BadRequestException.class, () -> emailVerificationService.verify(token.getToken()));

        assertFalse(customer.isEmailVerified());
        assertEquals(UserStatus.PENDING_VERIFICATION, customer.getStatus());
        verifyNoInteractions(customerActivationOutboxService);
    }

    @Test
    void requestingOtpDoesNotCreateAnActivationEvent() {
        User customer = user(false, UserStatus.PENDING_VERIFICATION);
        when(emailVerificationTokenRepository.save(org.mockito.ArgumentMatchers.any(EmailVerificationToken.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));

        emailVerificationService.create(customer);

        verifyNoInteractions(customerActivationOutboxService);
        assertFalse(customer.isEmailVerified());
    }

    private static User user(boolean verified, UserStatus status) {
        User user = new User("legacy-customer@example.com", "hash", "Customer", null);
        ReflectionTestUtils.setField(user, "id", 913L);
        user.setEmailVerified(verified);
        user.setStatus(status);
        return user;
    }

    private static EmailVerificationToken token(User user, EmailOtpPurpose purpose) {
        return new EmailVerificationToken(user, "test-otp", purpose, LocalDateTime.now().plusMinutes(1));
    }
}
