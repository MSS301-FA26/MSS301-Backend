package com.sba301.cinemaai.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "hero_banner_cinemas",
        uniqueConstraints = @UniqueConstraint(name = "uq_hero_banner_cinema", columnNames = {"hero_banner_id", "cinema_id"}),
        indexes = {
                @Index(name = "idx_hero_banner_cinemas_banner", columnList = "hero_banner_id"),
                @Index(name = "idx_hero_banner_cinemas_cinema", columnList = "cinema_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeroBannerCinema {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "hero_banner_id", nullable = false)
    private HeroBanner heroBanner;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @jakarta.persistence.Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt = LocalDateTime.now();

    public HeroBannerCinema(HeroBanner heroBanner, Cinema cinema) {
        this.heroBanner = heroBanner;
        this.cinema = cinema;
        this.createdAt = LocalDateTime.now();
    }
}
