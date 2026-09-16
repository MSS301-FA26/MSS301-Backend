package com.cinemaai.identity.entity;

import com.cinemaai.identity.enums.StaffStatus;
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
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "staff_profiles")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class StaffProfile extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Setter
    @Column(name = "cinema_id")
    private Long cinemaId;

    @Column(name = "employee_code", nullable = false, unique = true, length = 50)
    private String employeeCode;

    @Column(nullable = false, length = 100)
    private String position;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    @Setter
    private StaffStatus status = StaffStatus.ACTIVE;

    public StaffProfile(User user, Long cinemaId, String employeeCode, String position) {
        this.user = user;
        this.cinemaId = cinemaId;
        this.employeeCode = employeeCode;
        this.position = position;
    }

    public void updateDetails(String employeeCode, String position) {
        this.employeeCode = employeeCode;
        this.position = position;
    }
}
