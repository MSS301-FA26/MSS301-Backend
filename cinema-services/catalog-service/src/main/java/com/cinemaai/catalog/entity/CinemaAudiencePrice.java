package com.cinemaai.catalog.entity;

import com.cinemaai.catalog.enums.AudienceType;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Stores the additional price (surcharge) per audience type per cinema.
 *
 * <p>Final ticket price formula:
 * <pre>
 *   Standard/VIP seat: finalPrice = roomSeatBasePrice + additionalPrice(audienceType)
 *   Couple seat (pair): finalPrice = coupleBasePrice + additionalPrice(guest1) + additionalPrice(guest2)
 * </pre>
 *
 * <p>additionalPrice = 0 is valid (no surcharge for that audience type at this cinema).
 * A missing row means "not configured" and must block showtime saving.
 */
@Getter
@Entity
@Table(
        name = "cinema_audience_prices",
        uniqueConstraints = @UniqueConstraint(
                name = "uq_cinema_audience",
                columnNames = {"cinema_id", "audience_type"}
        )
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class CinemaAudiencePrice extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @Column(name = "cinema_id", nullable = false)
    private Long cinemaId;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "audience_type", nullable = false, length = 20)
    private AudienceType audienceType;

    /**
     * Additional price in VND added on top of the room's seat base price.
     * Must be >= 0. Zero is a valid configured value (no surcharge).
     */
    @Setter
    @Column(name = "additional_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal additionalPrice;

    public CinemaAudiencePrice(Long cinemaId, AudienceType audienceType, BigDecimal additionalPrice) {
        this.cinemaId = cinemaId;
        this.audienceType = audienceType;
        this.additionalPrice = additionalPrice;
    }
}
