package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.BannerCtaType;
import com.sba301.cinemaai.enums.BannerFocalPoint;
import com.sba301.cinemaai.enums.BannerScopeType;
import com.sba301.cinemaai.enums.BannerStatus;
import com.sba301.cinemaai.enums.BannerType;
import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Entity
@Table(
        name = "hero_banners",
        indexes = {
                @Index(name = "idx_hero_banners_status", columnList = "status"),
                @Index(name = "idx_hero_banners_schedule", columnList = "status, start_at, end_at"),
                @Index(name = "idx_hero_banners_sort", columnList = "sort_order, priority DESC"),
                @Index(name = "idx_hero_banners_type", columnList = "type"),
                @Index(name = "idx_hero_banners_scope", columnList = "scope_type"),
                @Index(name = "idx_hero_banners_movie", columnList = "linked_movie_id"),
                @Index(name = "idx_hero_banners_promotion", columnList = "linked_promotion_id")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class HeroBanner extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BannerType type = BannerType.MOVIE;

    @Column(nullable = false)
    private String title;

    @Column(length = 500)
    private String subtitle;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 100)
    private String badge;

    @Column(name = "format_label", length = 100)
    private String formatLabel;

    @Column(name = "desktop_image_url", nullable = false, length = 1000)
    private String desktopImageUrl;

    @Column(name = "mobile_image_url", length = 1000)
    private String mobileImageUrl;

    @Column(name = "alt_text")
    private String altText;

    @Enumerated(EnumType.STRING)
    @Column(name = "focal_point", nullable = false, length = 50)
    private BannerFocalPoint focalPoint = BannerFocalPoint.CENTER;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_movie_id")
    private Movie linkedMovie;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "linked_promotion_id")
    private Promotion linkedPromotion;

    @Enumerated(EnumType.STRING)
    @Column(name = "primary_cta_type", nullable = false, length = 50)
    private BannerCtaType primaryCtaType = BannerCtaType.BOOK_NOW;

    @Column(name = "primary_cta_label", nullable = false, length = 100)
    private String primaryCtaLabel = "ĐẶT VÉ NGAY";

    @Column(name = "primary_cta_url", length = 1000)
    private String primaryCtaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "secondary_cta_type", length = 50)
    private BannerCtaType secondaryCtaType = BannerCtaType.NONE;

    @Column(name = "secondary_cta_label", length = 100)
    private String secondaryCtaLabel;

    @Column(name = "secondary_cta_url", length = 1000)
    private String secondaryCtaUrl;

    @Enumerated(EnumType.STRING)
    @Column(name = "scope_type", nullable = false, length = 50)
    private BannerScopeType scopeType = BannerScopeType.GLOBAL;

    @Column(name = "target_city", length = 100)
    private String targetCity;

    @Column(nullable = false)
    private int priority = 1;

    @Column(name = "sort_order", nullable = false)
    private int sortOrder = 1;

    @Column(name = "slide_duration_seconds", nullable = false)
    private int slideDurationSeconds = 6;

    @Column(name = "start_at")
    private LocalDateTime startAt;

    @Column(name = "end_at")
    private LocalDateTime endAt;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 50)
    private BannerStatus status = BannerStatus.DRAFT;

    @Column(name = "impressions_count", nullable = false)
    private long impressionsCount = 0;

    @Column(name = "clicks_count", nullable = false)
    private long clicksCount = 0;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "created_by")
    private User createdBy;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "updated_by")
    private User updatedBy;

    @Column(name = "published_at")
    private LocalDateTime publishedAt;

    @OneToMany(mappedBy = "heroBanner", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<HeroBannerCinema> bannerCinemas = new ArrayList<>();

    public HeroBanner(
            String name,
            BannerType type,
            String title,
            String subtitle,
            String desktopImageUrl,
            String mobileImageUrl,
            BannerScopeType scopeType
    ) {
        this.name = name;
        this.type = type;
        this.title = title;
        this.subtitle = subtitle;
        this.desktopImageUrl = desktopImageUrl;
        this.mobileImageUrl = mobileImageUrl;
        this.scopeType = scopeType != null ? scopeType : BannerScopeType.GLOBAL;
        this.status = BannerStatus.DRAFT;
        this.focalPoint = BannerFocalPoint.CENTER;
        this.primaryCtaType = BannerCtaType.BOOK_NOW;
        this.primaryCtaLabel = "ĐẶT VÉ NGAY";
        this.slideDurationSeconds = 6;
        this.priority = 1;
        this.sortOrder = 1;
    }

    public void incrementImpression() {
        this.impressionsCount++;
    }

    public void incrementClick() {
        this.clicksCount++;
    }
}
