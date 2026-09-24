package com.cinemaai.booking.entity;

import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.SeatType;
import com.cinemaai.booking.enums.TicketType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "booking_seats",
        indexes = {
                @Index(name = "idx_booking_seats_showtime_seat", columnList = "showtime_id, seat_id"),
                @Index(name = "idx_booking_seats_booking", columnList = "booking_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingSeat extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "showtime_id", nullable = false)
    private Long showtimeId;

    @Column(name = "seat_id", nullable = false)
    private Long seatId;

    // Snapshot fields
    @Column(name = "row_label", nullable = false, length = 10)
    private String rowLabel;

    @Column(name = "seat_number", nullable = false)
    private int seatNumber;

    @Enumerated(EnumType.STRING)
    @Column(name = "seat_type", nullable = false, length = 30)
    private SeatType seatType;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private BookingSeatStatus status = BookingSeatStatus.HOLDING;

    @Column(name = "ticket_code", unique = true, length = 60)
    private String ticketCode;

    @Column(name = "qr_code", columnDefinition = "TEXT")
    private String qrCode;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", length = 30)
    private TicketType ticketType;

    @Column(name = "checked_in_at")
    private LocalDateTime checkedInAt;
}
