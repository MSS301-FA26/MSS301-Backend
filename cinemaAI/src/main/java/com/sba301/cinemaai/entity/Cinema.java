package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.CinemaStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "cinemas")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Cinema extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(nullable = false)
    private String name;

    @Setter
    @Column(nullable = false, length = 500)
    private String address;

    @Setter
    @Column(length = 100)
    private String city;

    @Setter
    @Column(length = 20)
    private String phone;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private CinemaStatus status = CinemaStatus.ACTIVE;

    public Cinema(String name, String address, String city, String phone) {
        this.name = name;
        this.address = address;
        this.city = city;
        this.phone = phone;
    }
}
