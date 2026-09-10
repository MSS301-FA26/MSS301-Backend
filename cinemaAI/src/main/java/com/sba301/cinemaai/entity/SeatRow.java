package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.SeatType;
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

@Getter
@Entity
@Table(name = "seat_rows")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class SeatRow extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "room_id", nullable = false)
    private Room room;

    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "display_order", nullable = false)
    private int displayOrder;

    @Column(name = "start_column", nullable = false)
    private int startColumn;

    @Enumerated(EnumType.STRING)
    @Column(name = "row_type", nullable = false, length = 30)
    private SeatType rowType = SeatType.STANDARD;

    public SeatRow(Room room, String rowLabel, int displayOrder, int startColumn, SeatType rowType) {
        this.room = room;
        this.rowLabel = rowLabel;
        this.displayOrder = displayOrder;
        this.startColumn = startColumn;
        this.rowType = rowType;
    }
}
