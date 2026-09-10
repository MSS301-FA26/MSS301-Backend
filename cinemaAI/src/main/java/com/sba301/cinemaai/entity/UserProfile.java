package com.sba301.cinemaai.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "user_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class UserProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Setter
    @Column(name = "full_name", nullable = false)
    private String fullName;

    @Setter
    @Column(length = 20)
    private String phone;

    @Setter
    @Column(name = "avatar_url", length = 500)
    private String avatarUrl;

    @Setter
    @Column(name = "phone_verified", nullable = false)
    private boolean phoneVerified;

    public UserProfile(User user, String fullName, String phone) {
        this.user = user;
        this.fullName = fullName;
        this.phone = phone;
    }
}
