package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.TicketType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.math.BigDecimal;
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
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 30)
    private TicketType ticketType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "room_type", nullable = false, length = 30)
    private RoomType roomType;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", length = 30)
    private SeatType seatType = SeatType.STANDARD;

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

    public TicketPricingRule(TicketType ticketType, RoomType roomType, SeatType seatType, boolean weekend, boolean holiday, BigDecimal price) {
        this.ticketType = ticketType;
        this.roomType = roomType;
        this.seatType = seatType == null ? SeatType.STANDARD : seatType;
        this.weekend = weekend;
        this.holiday = holiday;
        this.price = price;
    }
}
