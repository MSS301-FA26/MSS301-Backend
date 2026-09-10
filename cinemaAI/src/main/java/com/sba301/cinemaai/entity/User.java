package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.UserStatus;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "users")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class User extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String email;

    @Column(name = "password_hash", nullable = false)
    @Setter
    private String passwordHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter
    private UserStatus status = UserStatus.PENDING_VERIFICATION;

    @Column(name = "email_verified", nullable = false)
    @Setter
    private boolean emailVerified;

    @OneToOne(mappedBy = "user", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private UserProfile profile;

    @Setter
    @Column(name = "birth_year")
    private Integer birthYear;

    public User(String email, String passwordHash, String fullName, String phone) {
        this(email, passwordHash, fullName, phone, null);
    }

    public User(String email, String passwordHash, String fullName, String phone, Integer birthYear) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.profile = new UserProfile(this, fullName, phone);
        this.birthYear = birthYear;
    }

    public String getFullName() {
        return profile != null ? profile.getFullName() : null;
    }

    public String getPhone() {
        return profile != null ? profile.getPhone() : null;
    }

    public boolean isPhoneVerified() {
        return profile != null && profile.isPhoneVerified();
    }
}
