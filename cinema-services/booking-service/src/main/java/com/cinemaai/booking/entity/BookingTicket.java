package com.cinemaai.booking.entity;

import com.cinemaai.booking.enums.TicketType;
import jakarta.persistence.*;
import java.math.BigDecimal;
import lombok.*;

@Getter
@Setter
@Entity
@Table(
        name = "booking_tickets",
        indexes = {
                @Index(name = "idx_booking_tickets_booking", columnList = "booking_id")
        }
)
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class BookingTicket extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "booking_id", nullable = false)
    private Booking booking;

    @Column(name = "seat_id")
    private Long seatId;

    @Enumerated(EnumType.STRING)
    @Column(name = "ticket_type", nullable = false, length = 30)
    private TicketType ticketType;

    @Column(name = "viewer_age")
    private Integer viewerAge;

    @Builder.Default
    @Column(nullable = false)
    private int quantity = 1;

    @Builder.Default
    @Column(name = "unit_price", nullable = false, precision = 12, scale = 2)
    private BigDecimal unitPrice = BigDecimal.ZERO;

    @Builder.Default
    @Column(name = "line_total", nullable = false, precision = 12, scale = 2)
    private BigDecimal lineTotal = BigDecimal.ZERO;
}
