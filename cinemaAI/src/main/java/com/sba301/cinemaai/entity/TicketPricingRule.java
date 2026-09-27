package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
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
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(name = "ticket_pricing_rules")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class TicketPricingRule extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Setter
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "cinema_id")
    private Cinema cinema;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", length = 30)
    private TicketType ticketType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", length = 30)
    private RoomType roomType = RoomType.STANDARD;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 30)
    private SeatType seatType = SeatType.SINGLE;

    @Setter
    @Column(name = "weekend", nullable = false)
    private boolean weekend;

    @Setter
    @Column(name = "holiday", nullable = false)
    private boolean holiday;

    @Setter
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Setter
    @Column(nullable = false)
    private boolean active = true;

    @Setter
    @Column(name = "effective_from")
    private LocalDateTime effectiveFrom;

    @Setter
    @Column(name = "effective_to")
    private LocalDateTime effectiveTo;

    public TicketPricingRule(TicketType ticketType, RoomType roomType, SeatType seatType, boolean weekend, boolean holiday, BigDecimal price) {
        this.ticketType = ticketType;
        this.roomType = roomType == null ? RoomType.STANDARD : roomType;
        this.seatType = seatType == null ? SeatType.SINGLE : seatType;
        this.weekend = weekend;
        this.holiday = holiday;
        this.price = price;
        this.active = true;
    }

    public TicketPricingRule(Cinema cinema, SeatType seatType, BigDecimal price) {
        this.cinema = cinema;
        this.seatType = seatType == null ? SeatType.SINGLE : seatType;
        this.roomType = RoomType.STANDARD;
        this.price = price;
        this.active = true;
    }
}
