package com.cinemaai.payment.entity;

import com.cinemaai.payment.enums.LoyaltyStatus;
import jakarta.persistence.*;
import lombok.*;

@Getter
@Setter
@Entity
@Table(name = "loyalty_points")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoyaltyPoint extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "user_id", nullable = false, unique = true)
    private Long userId;

    @Column(name = "user_email")
    private String userEmail;

    @Builder.Default
    @Column(nullable = false)
    private int points = 0;

    @Builder.Default
    @Column(name = "total_points", nullable = false)
    private int totalPoints = 0;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private LoyaltyStatus status = LoyaltyStatus.ACTIVE;
}
