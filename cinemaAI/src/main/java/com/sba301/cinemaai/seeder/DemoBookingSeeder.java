package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.BookingTicket;
import com.sba301.cinemaai.entity.LoyaltyPoint;
import com.sba301.cinemaai.entity.LoyaltyPointTransaction;
import com.sba301.cinemaai.entity.Payment;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.LoyaltyPointType;
import com.sba301.cinemaai.enums.LoyaltyStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.PaymentProvider;
import com.sba301.cinemaai.enums.PaymentStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.BookingTicketRepository;
import com.sba301.cinemaai.repository.LoyaltyPointRepository;
import com.sba301.cinemaai.repository.LoyaltyPointTransactionRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.List;

/**
 * Seeds realistic demo bookings so the admin overview dashboard (revenue,
 * top movies, room occupancy, loyalty) always shows meaningful data right
 * after first boot.
 *
 * Strategy
 * --------
 * - 8 demo customer accounts (demo.customer1-8@cinemaai.com / Demo@123)
 * - Each customer: 3-4 PAID/USED bookings spread across NOW_SHOWING showtimes
 * - Each booking: 1-2 BookingSeats (CONFIRMED) + 1 Payment (SUCCESS) + 1 BookingTicket
 * - Each customer: LoyaltyPoint row + EARN transaction
 *
 * Idempotent: every booking is keyed on bookingCode.
 * Run order 55 - after CinemaScheduleSeeder (40) and DemoReviewSeeder (50).
 */
@Slf4j
@Component
@Order(55)
@RequiredArgsConstructor
public class DemoBookingSeeder implements Seeder {

    private static final String DEMO_PASSWORD = "Demo@123";
    private static final BigDecimal TICKET_PRICE = BigDecimal.valueOf(90_000);

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final ShowtimeRepository showtimeRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final BookingTicketRepository bookingTicketRepository;
    private final PaymentRepository paymentRepository;
    private final LoyaltyPointRepository loyaltyPointRepository;
    private final LoyaltyPointTransactionRepository loyaltyTxRepository;
    private final PasswordEncoder passwordEncoder;

    /** (email, fullName, phone) */
    private static final String[][] PERSONAS = {
        {"demo.customer1@cinemaai.com", "Nguyen Van An",    "0901000011"},
        {"demo.customer2@cinemaai.com", "Tran Thi Bao",     "0901000012"},
        {"demo.customer3@cinemaai.com", "Le Minh Chien",    "0901000013"},
        {"demo.customer4@cinemaai.com", "Pham Ngoc Diem",   "0901000014"},
        {"demo.customer5@cinemaai.com", "Vo Hoang Gia",     "0901000015"},
        {"demo.customer6@cinemaai.com", "Dang Hai Ha",      "0901000016"},
        {"demo.customer7@cinemaai.com", "Bui Truong Khanh", "0901000017"},
        {"demo.customer8@cinemaai.com", "Hoang Yen Linh",   "0901000018"},
    };

    @Override
    @Transactional
    public void seed() {
        List<Showtime> showtimes = showtimeRepository.findAll().stream()
            .filter(st -> st.getMovie().getStatus() == MovieStatus.NOW_SHOWING)
            .toList();

        if (showtimes.isEmpty()) {
            log.warn("[DemoBookingSeeder] No NOW_SHOWING showtimes found - skipping.");
            return;
        }

        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
            .orElseGet(() -> roleRepository.save(new Role(RoleName.CUSTOMER)));

        int showtimeIdx = 0;
        for (int p = 0; p < PERSONAS.length; p++) {
            User user = resolveUser(PERSONAS[p][0], PERSONAS[p][1], PERSONAS[p][2], customerRole);

            int bookingsPerUser = (p % 2 == 0) ? 4 : 3;
            for (int b = 0; b < bookingsPerUser; b++) {
                Showtime showtime = showtimes.get(showtimeIdx % showtimes.size());
                showtimeIdx++;

                String code = "DMO" + (p + 1) + "B" + (b + 1) + "S" + showtime.getId();
                if (bookingRepository.existsByBookingCode(code)) {
                    continue;
                }
                seedBookingWithPayment(user, showtime, code, p, b);
            }

            ensureLoyaltyRow(user);
        }

        log.info("[DemoBookingSeeder] Demo booking seed complete.");
    }

    private User resolveUser(String email, String fullName, String phone, Role role) {
        User user = userRepository.findByEmail(email).orElseGet(() -> {
            User u = new User(email, passwordEncoder.encode(DEMO_PASSWORD), fullName, phone);
            u.setEmailVerified(true);
            u.setStatus(UserStatus.ACTIVE);
            return userRepository.save(u);
        });
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
            userRoleRepository.save(new UserRole(user, role));
        }
        return user;
    }

    private void seedBookingWithPayment(User user, Showtime showtime,
                                        String code, int personaIdx, int bookingIdx) {
        LocalDateTime paidAt = LocalDateTime.now()
            .minusDays(29 - (long)(personaIdx * 3 + bookingIdx) % 28)
            .withHour(10 + bookingIdx * 2)
            .withMinute(30).withSecond(0).withNano(0);

        int seats = (bookingIdx % 2 == 0) ? 2 : 1;
        BigDecimal unitPrice = resolveUnitPrice(showtime);
        BigDecimal subtotal  = unitPrice.multiply(BigDecimal.valueOf(seats));

        Booking booking = new Booking(code, user, showtime, paidAt.plusMinutes(10));
        booking.setSubtotal(subtotal);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(subtotal);
        booking.setStatus(BookingStatus.USED);
        booking.setPaidAt(paidAt);
        booking.setCheckedInAt(showtime.getStartTime().minusMinutes(10));
        booking.setQrCode("DEMO-QR-" + code);
        booking = bookingRepository.save(booking);

        bookingTicketRepository.save(new BookingTicket(
            booking, TicketType.ADULT, 25, seats, unitPrice, subtotal
        ));

        List<Seat> roomSeats = seatRepository.findByRoom(showtime.getRoom());
        int taken = 0;
        for (Seat seat : roomSeats) {
            if (taken >= seats) break;
            boolean occupied = !bookingSeatRepository
                .findByShowtimeAndSeatAndStatusIn(
                    showtime, seat,
                    List.of(SeatRuntimeStatus.BOOKED, SeatRuntimeStatus.HOLDING)
                ).isEmpty();
            if (occupied) continue;

            BookingSeat bs = new BookingSeat(booking, showtime, seat, unitPrice);
            bs.setStatus(SeatRuntimeStatus.CHECKED_IN);
            bs.setTicketType(TicketType.ADULT);
            bs.setTicketCode(code + "-" + seat.getRowLabel() + seat.getSeatNumber());
            bs.setQrCode("CINEAI:SEAT:" + code + "-" + seat.getRowLabel() + seat.getSeatNumber() + ":" + user.getId());
            bs.setCheckedInAt(showtime.getStartTime().minusMinutes(10));
            bookingSeatRepository.save(bs);
            taken++;
        }

        Payment payment = new Payment(booking, PaymentProvider.VNPAY, subtotal);
        payment.setStatus(PaymentStatus.SUCCESS);
        payment.setTransactionId("DEMO-TXN-" + code);
        payment.setPaidAt(paidAt);

        paymentRepository.save(payment);

        int earnedPoints = subtotal.divide(BigDecimal.valueOf(1000), 0, RoundingMode.FLOOR).intValue();
        if (earnedPoints > 0) {
            LoyaltyPoint lp = loyaltyPointRepository.findByUser(user)
                .orElseGet(() -> loyaltyPointRepository.save(new LoyaltyPoint(user)));
            lp.setPoints(lp.getPoints() + earnedPoints);
            lp.setTotalPoints(lp.getTotalPoints() + earnedPoints);
            loyaltyPointRepository.save(lp);

            loyaltyTxRepository.save(new LoyaltyPointTransaction(
                user, booking, LoyaltyPointType.EARN,
                earnedPoints, lp.getPoints(),
                "Tich diem dat ve " + code
            ));
        }
    }

    private void ensureLoyaltyRow(User user) {
        if (!loyaltyPointRepository.existsByUserId(user.getId())) {
            LoyaltyPoint lp = new LoyaltyPoint(user);
            lp.setStatus(LoyaltyStatus.ACTIVE);
            loyaltyPointRepository.save(lp);
        }
    }

    private BigDecimal resolveUnitPrice(Showtime st) {
        if (st.getAdultStandardPrice() != null) return st.getAdultStandardPrice();
        if (st.getBasePrice()          != null) return st.getBasePrice();
        return TICKET_PRICE;
    }
}