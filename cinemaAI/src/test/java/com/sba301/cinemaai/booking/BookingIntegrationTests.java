package com.sba301.cinemaai.booking;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.dto.request.booking.BookingFoodRequest;
import com.sba301.cinemaai.dto.request.booking.CheckInRequest;
import com.sba301.cinemaai.dto.request.booking.CreateBookingRequest;
import com.sba301.cinemaai.dto.request.booking.HoldSeatsRequest;
import com.sba301.cinemaai.dto.request.food.FoodItemRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketPricingRuleRequest;
import com.sba301.cinemaai.dto.request.ticket.TicketSelectionRequest;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.SeatRow;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.AgeRating;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.FoodItemStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.enums.TicketType;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.RoomRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.SeatRowRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import com.sba301.cinemaai.service.BookingService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class BookingIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MovieRepository movieRepository;

    @Autowired
    private CinemaRepository cinemaRepository;

    @Autowired
    private RoomRepository roomRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private SeatRowRepository seatRowRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private BookingService bookingService;

    @Test
    void shouldHoldBookPreventDuplicateSeatAndCheckInByQr() throws Exception {
        String adminToken = loginAs("phase6.admin.", RoleName.ADMIN);
        String customerToken = loginAs("phase6.customer.", RoleName.CUSTOMER);
        String anotherCustomerToken = loginAs("phase6.other.", RoleName.CUSTOMER);

        Long foodItemId = createFoodItem(adminToken);
        Showtime showtime = createShowtimeFixture();
        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);

        String holdResponse = mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(firstSeat.getId())
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLDING"))
                .andExpect(jsonPath("$.data.seats[0].status").value("HOLDING"))
                .andReturn()
                .getResponse()
                .getContentAsString();

        Long holdBookingId = objectMapper.readTree(holdResponse).at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + anotherCustomerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(firstSeat.getId())
                        ))))
                .andExpect(status().isConflict());

        String bookingResponse = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                holdBookingId,
                                List.of(new BookingFoodRequest(foodItemId, null, 2))
                ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(155000))
                .andReturn()
                .getResponse()
                .getContentAsString();

        JsonNode bookingJson = objectMapper.readTree(bookingResponse);
        Long bookingId = bookingJson.at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"));

        String paidBookingResponse = mockMvc.perform(post("/api/v1/payments/mock")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        assertThat(objectMapper.readTree(paidBookingResponse).at("/data/bookingId").asLong()).isEqualTo(bookingId);

        String paidAdminBookingResponse = mockMvc.perform(get("/api/v1/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PAID"))
                .andExpect(jsonPath("$.data.qrCode").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();
        String qrCode = objectMapper.readTree(paidAdminBookingResponse).at("/data/qrCode").asText();

        mockMvc.perform(get("/api/v1/admin/bookings")
                .header("Authorization", "Bearer " + adminToken)
                .param("status", "PAID"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("PAID"));

        mockMvc.perform(get("/api/v1/showtimes/{showtimeId}/seat-map", showtime.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.seats[0].runtimeStatus").value("BOOKED"));

        mockMvc.perform(post("/api/v1/admin/check-in")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CheckInRequest(qrCode))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("USED"))
                .andExpect(jsonPath("$.data.seats[0].status").value("CHECKED_IN"));

        mockMvc.perform(get("/api/v1/foods/items"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.items[0].status").value("ACTIVE"));

        mockMvc.perform(delete("/api/v1/admin/foods/items/{itemId}", foodItemId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("OUT_OF_STOCK"));
    }

    @Test
    void shouldReleaseExpiredSeatHolds() {
        User customer = createUser("phase6.expired.", RoleName.CUSTOMER);
        Showtime showtime = createShowtimeFixture();
        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);
        Booking expiredHold = bookingRepository.save(new Booking(
                "BKEXPIRED" + System.nanoTime(),
                customer,
                showtime,
                LocalDateTime.now().minusMinutes(1)
        ));
        BookingSeat heldSeat = bookingSeatRepository.save(new BookingSeat(
                expiredHold,
                showtime,
                firstSeat,
                showtime.getBasePrice()
        ));

        int releasedCount = bookingService.releaseExpiredHolds();

        Booking updatedBooking = bookingRepository.findById(expiredHold.getId()).orElseThrow();
        BookingSeat updatedSeat = bookingSeatRepository.findById(heldSeat.getId()).orElseThrow();
        assertThat(releasedCount).isGreaterThanOrEqualTo(1);
        assertThat(updatedBooking.getStatus()).isEqualTo(BookingStatus.EXPIRED);
        assertThat(updatedSeat.getStatus()).isEqualTo(SeatRuntimeStatus.RELEASED);
    }

    @Test
    void shouldAllowOnlyOneActiveCheckoutPerCustomerAndShowtime() throws Exception {
        String customerToken = loginAs("phase5.single.checkout.", RoleName.CUSTOMER);
        Showtime showtime = createShowtimeFixture();
        List<Seat> seats = seatRepository.findByRoom(showtime.getRoom());
        Seat firstSeat = seats.get(0);
        Seat secondSeat = seats.get(1);

        String firstHoldResponse = mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(firstSeat.getId())
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long firstBookingId = objectMapper.readTree(firstHoldResponse).at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(secondSeat.getId())
                        ))))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value(
                        "Bạn đã có đơn đang giữ ghế cho suất chiếu này. Hãy tiếp tục thanh toán hoặc hủy đơn trước khi chọn ghế mới"
                ));

        mockMvc.perform(delete("/api/v1/bookings/{bookingId}", firstBookingId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(secondSeat.getId())
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("HOLDING"));
    }

    @Test
    void shouldCreateBookingWithTicketValidationAndRefundFlow() throws Exception {
        String adminToken = loginAs("phase6.ticket.admin.", RoleName.ADMIN);
        String customerToken = loginAs("phase6.ticket.customer.", RoleName.CUSTOMER);
        Showtime showtime = createShowtimeFixture();
        createAdultTicketRule(adminToken);
        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);

        String holdResponse = mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(firstSeat.getId())
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long holdBookingId = objectMapper.readTree(holdResponse).at("/data/id").asLong();

        String bookingResponse = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                holdBookingId,
                                List.of(),
                                null,
                                false,
                                List.of(new TicketSelectionRequest(TicketType.ADULT, 20, 1)),
                                null
                        ))))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.totalAmount").value(95000))
                .andExpect(jsonPath("$.data.tickets[0].ticketType").value("ADULT"))
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long bookingId = objectMapper.readTree(bookingResponse).at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/payments/mock")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        mockMvc.perform(post("/api/v1/admin/showtimes/{showtimeId}/cancel", showtime.getId())
                        .header("Authorization", "Bearer " + adminToken)
                        .param("reason", "Cúp điện trong rạp"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        mockMvc.perform(get("/api/v1/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refundReason").value("Hủy suất chiếu do sự cố: Cúp điện trong rạp"))
                .andExpect(jsonPath("$.data.qrCode").doesNotExist())
                .andExpect(jsonPath("$.data.paymentAccount").doesNotExist());
    }

    @Test
    void adminCancelPaidBookingShouldRefundToCineWallet() throws Exception {
        String adminToken = loginAs("phase6.refund.admin.", RoleName.ADMIN);
        String customerToken = loginAs("phase6.refund.customer.", RoleName.CUSTOMER);
        Showtime showtime = createShowtimeFixture();
        createAdultTicketRule(adminToken);
        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);

        Long bookingId = createPaidBooking(customerToken, showtime, firstSeat);

        // Admin hủy vé đã thanh toán -> tiền hoàn về CineWallet của khách
        mockMvc.perform(delete("/api/v1/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + adminToken)
                        .param("reason", "Khách yêu cầu hủy"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("REFUNDED"))
                .andExpect(jsonPath("$.data.refundedAt").isNotEmpty())
                .andExpect(jsonPath("$.data.refundReason").value("Admin hoàn/hủy vé: Khách yêu cầu hủy"))
                .andExpect(jsonPath("$.data.qrCode").doesNotExist());

        // Ghế được giải phóng
        Booking refunded = bookingRepository.findById(bookingId).orElseThrow();
        assertThat(refunded.getStatus()).isEqualTo(BookingStatus.REFUNDED);
        assertThat(refunded.getRefundMethod()).isEqualTo("CINEWALLET");

        // Số dư ví của khách được ghi có đúng totalAmount
        mockMvc.perform(get("/api/v1/wallet")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.balance").value(95000));
    }

    @Test
    void adminCancelCheckedInBookingShouldBeRejected() throws Exception {
        String adminToken = loginAs("phase6.used.admin.", RoleName.ADMIN);
        String customerToken = loginAs("phase6.used.customer.", RoleName.CUSTOMER);
        Showtime showtime = createShowtimeFixture();
        createAdultTicketRule(adminToken);
        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);

        Long bookingId = createPaidBooking(customerToken, showtime, firstSeat);

        // Ép trạng thái USED (đã check-in) — không được phép hoàn
        Booking booking = bookingRepository.findById(bookingId).orElseThrow();
        booking.setStatus(BookingStatus.USED);
        bookingRepository.save(booking);

        mockMvc.perform(delete("/api/v1/admin/bookings/{bookingId}", bookingId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isBadRequest());

        assertThat(bookingRepository.findById(bookingId).orElseThrow().getStatus())
                .isEqualTo(BookingStatus.USED);
    }

    private Long createPaidBooking(String customerToken, Showtime showtime, Seat seat) throws Exception {
        String holdResponse = mockMvc.perform(post("/api/v1/bookings/hold")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new HoldSeatsRequest(
                                showtime.getId(),
                                List.of(seat.getId())
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long holdBookingId = objectMapper.readTree(holdResponse).at("/data/id").asLong();

        String bookingResponse = mockMvc.perform(post("/api/v1/bookings")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new CreateBookingRequest(
                                holdBookingId,
                                List.of(),
                                null,
                                false,
                                List.of(new TicketSelectionRequest(TicketType.ADULT, 20, 1)),
                                null
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        Long bookingId = objectMapper.readTree(bookingResponse).at("/data/id").asLong();

        mockMvc.perform(post("/api/v1/payments/mock")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", bookingId.toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        return bookingId;
    }

    private Long createFoodItem(String adminToken) throws Exception {
        String response = mockMvc.perform(post("/api/v1/admin/foods/items")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new FoodItemRequest(
                                "Phase 6 Popcorn " + System.nanoTime(),
                                "Booking test snack",
                                BigDecimal.valueOf(30000),
                                "https://example.com/popcorn.jpg",
                                FoodItemStatus.ACTIVE
                        ))))
                .andExpect(status().isCreated())
                .andReturn()
                .getResponse()
                .getContentAsString();
        return objectMapper.readTree(response).at("/data/id").asLong();
    }

    private void createAdultTicketRule(String adminToken) throws Exception {
        int status = mockMvc.perform(post("/api/v1/admin/ticket-pricing/rules")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new TicketPricingRuleRequest(
                                TicketType.ADULT,
                                RoomType.TWO_D,
                                SeatType.STANDARD,
                                false,
                                false,
                                BigDecimal.valueOf(95000),
                                true
                        ))))
                .andReturn()
                .getResponse()
                .getStatus();
        // 201 = tạo mới; 409 = quy tắc đã tồn tại (DB dùng chung giữa các test) — cả hai đều hợp lệ
        assertThat(status).isIn(201, 409);
    }

    private Showtime createShowtimeFixture() {
        String suffix = Long.toString(System.nanoTime());
        Movie movie = new Movie("Phase 6 Movie " + suffix, 110, MovieStatus.NOW_SHOWING);
        movie.setDescription("Booking flow movie.");
        movie.setReleaseDate(LocalDate.of(2026, 5, 19));
        movie.setLanguage("English");
        movie.setSubtitleLanguage("Vietnamese");
        movie.setAgeRating(AgeRating.from("13+"));
        movie.setDirector("Phase Six Director");
        movie.setMainActors("Phase Six Lead");
        movie.setCastList("Cast");
        Movie savedMovie = movieRepository.save(movie);

        Cinema cinema = cinemaRepository.save(new Cinema("Phase 6 Cinema " + suffix, "1 Booking Street", "HCMC", "0900666777"));
        Room room = roomRepository.save(new Room(cinema, "Room 6", RoomType.TWO_D, 1, 2));
        SeatRow seatRow = seatRowRepository.save(new SeatRow(room, "A", 1, 1, SeatType.NORMAL));
        seatRepository.save(new Seat(room, seatRow, 1, 1, SeatType.NORMAL));
        seatRepository.save(new Seat(room, seatRow, 2, 2, SeatType.NORMAL));

        LocalDateTime startTime = LocalDateTime.now().plusMinutes(5);
        Showtime showtime = new Showtime(
                savedMovie,
                room,
                startTime,
                startTime.plusMinutes(125),
                BigDecimal.valueOf(95000)
        );
        // Keep fixture pricing deterministic even when the suite runs after 22:00.
        showtime.setLateNightSurchargeAmount(BigDecimal.ZERO);
        showtime.setStatus(ShowtimeStatus.OPEN);
        return showtimeRepository.save(showtime);
    }

    private String loginAs(String prefix, RoleName roleName) throws Exception {
        String email = prefix + System.nanoTime() + "@example.com";
        String password = "Password123";
        User savedUser = createUser(email, password, roleName);

        String response = mockMvc.perform(post("/api/v1/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(new LoginRequest(savedUser.getEmail(), password))))
                .andExpect(status().isOk())
                .andReturn()
                .getResponse()
                .getContentAsString();

        return objectMapper.readTree(response).at("/data/accessToken").asText();
    }

    private User createUser(String prefix, RoleName roleName) {
        return createUser(prefix + System.nanoTime() + "@example.com", "Password123", roleName);
    }

    private User createUser(String email, String password, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User(email, passwordEncoder.encode(password), "Phase Six User", "0900666888");
        user.setEmailVerified(true);
        user.setStatus(com.sba301.cinemaai.enums.UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser, role));
        return savedUser;
    }
}
