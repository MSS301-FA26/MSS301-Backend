package com.sba301.cinemaai.entity;

import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.TicketType;
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
import jakarta.persistence.Table;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Entity
@Table(
        name = "booking_seats",
        indexes = {
                @Index(name = "idx_booking_seats_booking", columnList = "booking_id"),
                @Index(name = "idx_booking_seats_showtime_seat_status", columnList = "showtime_id,seat_id,status")
        }
)
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class BookingSeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "showtime_id", nullable = false)
    private Showtime showtime;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "seat_id", nullable = false)
    private Seat seat;

    @Setter
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SeatRuntimeStatus status = SeatRuntimeStatus.HOLDING;

    @Setter
    @Column(name = "ticket_code", unique = true, length = 60)
    private String ticketCode;

    @Setter
    @Column(name = "qr_code", length = 500)
    private String qrCode;

    @Setter
    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", length = 30)
    private TicketType ticketType;

    @Setter
    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;

    public BookingSeat(Booking booking, Showtime showtime, Seat seat, BigDecimal unitPrice) {
        this.booking = booking;
        this.showtime = showtime;
        this.seat = seat;
        this.unitPrice = unitPrice;
    }
}
