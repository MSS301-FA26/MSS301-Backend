package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.RoomStatus;
import com.sba301.cinemaai.enums.RoomType;
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
@Table(name = "rooms")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Room extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

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

    public Room(Cinema cinema, String name, RoomType roomType, int rowCount, int columnCount) {
        this.cinema = cinema;
        this.name = name;
        this.roomType = roomType;
        this.rowCount = rowCount;
        this.columnCount = columnCount;
    }
}
