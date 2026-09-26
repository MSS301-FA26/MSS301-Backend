package com.cinemaai.catalog.entity;

import com.cinemaai.catalog.enums.RoomStatus;
import com.cinemaai.catalog.enums.RoomType;
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
import java.math.BigDecimal;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "cinema_id", nullable = false)
    private Cinema cinema;

    @Setter
    @Column(nullable = false, length = 100)
    private String name;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private RoomType roomType = RoomType.STANDARD;

    @Setter
    @Column(name = "row_count", nullable = false)
    private int rowCount;

    @Setter
    @Column(name = "column_count", nullable = false)
    private int columnCount;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private RoomStatus status = RoomStatus.ACTIVE;

    @Setter
    @Column(name = "standard_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal standardPrice = BigDecimal.valueOf(60000);

    @Setter
    @Column(name = "vip_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal vipPrice = BigDecimal.valueOf(90000);

    @Setter
    @Column(name = "couple_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal couplePrice = BigDecimal.valueOf(150000);

    @Setter
    @Column(name = "aisle_position", nullable = false)
    private int aislePosition = 0;

    public Room(Cinema cinema, String name, RoomType roomType, int rowCount, int columnCount,
                BigDecimal standardPrice, BigDecimal vipPrice, BigDecimal couplePrice, Integer aislePosition) {
        this.cinema = cinema;
        this.name = name;
        this.roomType = roomType;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
        if (standardPrice != null) this.standardPrice = standardPrice;
        if (vipPrice != null) this.vipPrice = vipPrice;
        if (couplePrice != null) this.couplePrice = couplePrice;
        if (aislePosition != null) this.aislePosition = aislePosition;
    }

    public Room(Cinema cinema, String name, RoomType roomType, int rowCount, int columnCount,
                BigDecimal standardPrice, BigDecimal vipPrice, BigDecimal couplePrice) {
        this(cinema, name, roomType, rowCount, columnCount, standardPrice, vipPrice, couplePrice, 0);
    }

    public Room(Cinema cinema, String name, RoomType roomType, int rowCount, int columnCount) {
        this(cinema, name, roomType, rowCount, columnCount,
                BigDecimal.valueOf(60000), BigDecimal.valueOf(90000), BigDecimal.valueOf(150000), 0);
    }
}
