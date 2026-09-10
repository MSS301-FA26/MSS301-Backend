package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.EmailOtpPurpose;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "email_verification_tokens")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class EmailVerificationToken extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, unique = true, length = 500)
    private String token;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private EmailOtpPurpose purpose = EmailOtpPurpose.EMAIL_VERIFICATION;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Setter
    @Column(nullable = false)
    private boolean used;

    public EmailVerificationToken(User user, String token, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.expiresAt = expiresAt;
    }

    public EmailVerificationToken(User user, String token, EmailOtpPurpose purpose, LocalDateTime expiresAt) {
        this.user = user;
        this.token = token;
        this.purpose = purpose;
        this.expiresAt = expiresAt;
    }
}
