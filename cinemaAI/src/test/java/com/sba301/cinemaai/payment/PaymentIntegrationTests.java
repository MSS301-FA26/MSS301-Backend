package com.sba301.cinemaai.payment;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.sba301.cinemaai.dto.request.auth.LoginRequest;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.FoodItem;
import com.sba301.cinemaai.entity.FoodOrder;
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
import com.sba301.cinemaai.enums.FoodOrderStatus;
import com.sba301.cinemaai.enums.MovieStatus;
import com.sba301.cinemaai.enums.PaymentStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatRuntimeStatus;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.BookingSeatRepository;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.FoodItemRepository;
import com.sba301.cinemaai.repository.FoodOrderRepository;
import com.sba301.cinemaai.repository.LoyaltyPointRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.PaymentRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.RoomRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.SeatRowRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PaymentIntegrationTests {

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
    private SeatRowRepository seatRowRepository;

    @Autowired
    private SeatRepository seatRepository;

    @Autowired
    private ShowtimeRepository showtimeRepository;

    @Autowired
    private BookingRepository bookingRepository;

    @Autowired
    private BookingSeatRepository bookingSeatRepository;

    @Autowired
    private PaymentRepository paymentRepository;

    @Autowired
    private FoodItemRepository foodItemRepository;

    @Autowired
    private FoodOrderRepository foodOrderRepository;

    @Autowired
    private LoyaltyPointRepository loyaltyPointRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private RoleRepository roleRepository;

    @Autowired
    private UserRoleRepository userRoleRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @TestConfiguration
    static class PaymentTestSecurityConfig {

        @Bean
        @ConditionalOnMissingBean(PasswordEncoder.class)
        PasswordEncoder passwordEncoder() {
            return new BCryptPasswordEncoder(10);
        }
    }

    @Test
    void shouldCreateVnpayPaymentUrlAndSupersedePendingPaymentOnRetry() throws Exception {
        String customerToken = loginAs("phase6.payment.vnpay.", RoleName.CUSTOMER);
        User customer = userRepository.findAll().stream()
                .filter(user -> user.getEmail().startsWith("phase6.payment.vnpay."))
                .findFirst()
                .orElseThrow();
        Booking booking = createPendingBooking(customer);

        String paymentResponse = mockMvc.perform(post("/api/v1/payments/vnpay/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", booking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("VNPAY"))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        String paymentUrl = objectMapper.readTree(paymentResponse).at("/data/paymentUrl").asText();
        assertThat(paymentUrl).contains("vnp_TxnRef=", "vnp_IpAddr=", "vnp_ExpireDate=", "vnp_SecureHash=");

        mockMvc.perform(post("/api/v1/payments/vnpay/create")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", booking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("VNPAY"))
                .andExpect(jsonPath("$.data.status").value("PENDING"));

        assertThat(paymentRepository.findByBooking(booking))
                .extracting(payment -> payment.getStatus())
                .containsExactlyInAnyOrder(PaymentStatus.FAILED, PaymentStatus.PENDING);

        mockMvc.perform(get("/api/v1/payments/booking/{bookingId}", booking.getId())
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PENDING"));
    }

    @Test
    void shouldMockPayBookingAndGenerateQrForCheckIn() throws Exception {
        String customerToken = loginAs("phase6.payment.mock.", RoleName.CUSTOMER);
        User customer = userRepository.findAll().stream()
                .filter(user -> user.getEmail().startsWith("phase6.payment.mock."))
                .findFirst()
                .orElseThrow();
        Booking booking = createPendingBooking(customer);

        mockMvc.perform(post("/api/v1/payments/mock")
                        .header("Authorization", "Bearer " + customerToken)
                        .param("bookingId", booking.getId().toString()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.provider").value("MOCK"))
                .andExpect(jsonPath("$.data.status").value("SUCCESS"))
                .andExpect(jsonPath("$.data.transactionId").isNotEmpty())
                .andExpect(jsonPath("$.data.paidAt").isNotEmpty());

        Booking paidBooking = bookingRepository.findById(booking.getId()).orElseThrow();
        assertThat(paidBooking.getStatus()).isEqualTo(BookingStatus.PAID);
        assertThat(paidBooking.getQrCode()).isNotBlank();
        assertThat(paymentRepository.findByBooking(paidBooking))
                .anySatisfy(payment -> assertThat(payment.getStatus()).isEqualTo(PaymentStatus.SUCCESS));
        assertThat(bookingSeatRepository.findByBooking(paidBooking))
                .allSatisfy(seat -> assertThat(seat.getStatus()).isEqualTo(SeatRuntimeStatus.BOOKED));
    }

    @Test
    void shouldRejectInvalidVnpayCallbackSignatureAndRedirectReturnUrl() throws Exception {
        mockMvc.perform(get("/api/v1/payments/vnpay/ipn")
                        .param("vnp_TxnRef", "1-BKINVALID")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_TransactionStatus", "00")
                        .param("vnp_Amount", "12500000")
                        .param("vnp_SecureHash", "bad-signature"))
                .andExpect(status().isOk())
                .andExpect(content().string("{\"RspCode\":\"97\",\"Message\":\"Invalid signature\"}"));

        mockMvc.perform(get("/api/v1/payments/vnpay/return")
                        .param("vnp_TxnRef", "1-BKINVALID")
                        .param("vnp_ResponseCode", "00")
                        .param("vnp_SecureHash", "bad-signature"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.startsWith("http://localhost:3000/payment-callback")));
    }

    @Test
    void shouldCreateAndPayStandaloneFoodOrderWithoutBooking() throws Exception {
        String customerToken = loginAs("phase6.food.standalone.", RoleName.CUSTOMER);
        FoodItem foodItem = foodItemRepository.save(new FoodItem(
                "Standalone popcorn " + System.nanoTime(),
                "Counter pickup",
                BigDecimal.valueOf(55000)
        ));

        String createResponse = mockMvc.perform(post("/api/v1/food-orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foods":[{"foodItemId":%d,"foodComboId":null,"quantity":2}]}
                                """.formatted(foodItem.getId())))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data.bookingId").isEmpty())
                .andExpect(jsonPath("$.data.orderCode").isNotEmpty())
                .andExpect(jsonPath("$.data.totalAmount").value(110000))
                .andExpect(jsonPath("$.data.status").value("PENDING_PAYMENT"))
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                .andReturn()
                .getResponse()
                .getContentAsString();

        long foodOrderId = objectMapper.readTree(createResponse).at("/data/id").asLong();
        mockMvc.perform(post("/api/v1/payments/food-orders/{foodOrderId}/vnpay/create", foodOrderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").isEmpty())
                .andExpect(jsonPath("$.data.provider").value("VNPAY"))
                .andExpect(jsonPath("$.data.paymentUrl").isNotEmpty());

        mockMvc.perform(post("/api/v1/payments/food-orders/{foodOrderId}/mock", foodOrderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.bookingId").isEmpty())
                .andExpect(jsonPath("$.data.status").value("SUCCESS"));

        FoodOrder paidOrder = foodOrderRepository.findById(foodOrderId).orElseThrow();
        assertThat(paidOrder.getBooking()).isNull();
        assertThat(paidOrder.getCustomer()).isNotNull();
        assertThat(paidOrder.getStatus()).isEqualTo(FoodOrderStatus.PAID);
        assertThat(loyaltyPointRepository.findByUser(paidOrder.getCustomer()).orElseThrow().getPoints())
                .isPositive();

        String ordersResponse = mockMvc.perform(get("/api/v1/food-orders/my")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("PAID"))
                .andExpect(jsonPath("$.data[0].qrCode").value(org.hamcrest.Matchers.startsWith("CINEAI:FOOD:")))
                .andReturn().getResponse().getContentAsString();
        String pickupQr = objectMapper.readTree(ordersResponse).at("/data/0/qrCode").asText();
        String staffToken = loginAs("phase6.food.pickup.staff.", RoleName.STAFF);

        mockMvc.perform(get("/api/v1/staff/check-in/food-orders/lookup")
                        .param("code", pickupQr)
                        .header("Authorization", "Bearer " + staffToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.id").value(foodOrderId))
                .andExpect(jsonPath("$.data.status").value("PAID"));

        mockMvc.perform(post("/api/v1/staff/check-in/food-orders/pickup")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", pickupQr))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("PICKED_UP"))
                .andExpect(jsonPath("$.data.pickedUpAt").isNotEmpty())
                .andExpect(jsonPath("$.data.qrCode").isEmpty());

        mockMvc.perform(post("/api/v1/staff/check-in/food-orders/pickup")
                        .header("Authorization", "Bearer " + staffToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(java.util.Map.of("code", pickupQr))))
                .andExpect(status().isConflict());
    }

    @Test
    void shouldListAndCancelPendingStandaloneFoodOrder() throws Exception {
        String customerToken = loginAs("phase6.food.cancel.", RoleName.CUSTOMER);
        FoodItem foodItem = foodItemRepository.save(new FoodItem(
                "Standalone water " + System.nanoTime(),
                "Counter pickup",
                BigDecimal.valueOf(20000)
        ));

        String createResponse = mockMvc.perform(post("/api/v1/food-orders")
                        .header("Authorization", "Bearer " + customerToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"foods":[{"foodItemId":%d,"foodComboId":null,"quantity":1}]}
                                """.formatted(foodItem.getId())))
                .andExpect(status().isCreated())
                .andReturn().getResponse().getContentAsString();
        long foodOrderId = objectMapper.readTree(createResponse).at("/data/id").asLong();

        mockMvc.perform(get("/api/v1/food-orders/my")
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(foodOrderId))
                .andExpect(jsonPath("$.data[0].status").value("PENDING_PAYMENT"));

        mockMvc.perform(delete("/api/v1/food-orders/{foodOrderId}", foodOrderId)
                        .header("Authorization", "Bearer " + customerToken))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.status").value("CANCELLED"));

        assertThat(foodOrderRepository.findById(foodOrderId).orElseThrow().getStatus())
                .isEqualTo(FoodOrderStatus.CANCELLED);
    }

    private Booking createPendingBooking(User customer) {
        Showtime showtime = createShowtimeFixture();
        Booking booking = new Booking("BKPAY" + System.nanoTime(), customer, showtime, LocalDateTime.now().plusMinutes(10));
        booking.setSubtotal(BigDecimal.valueOf(125000));
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(BigDecimal.valueOf(125000));
        booking.setStatus(BookingStatus.PENDING_PAYMENT);
        Booking savedBooking = bookingRepository.save(booking);

        Seat firstSeat = seatRepository.findByRoom(showtime.getRoom()).get(0);
        bookingSeatRepository.save(new com.sba301.cinemaai.entity.BookingSeat(
                savedBooking,
                showtime,
                firstSeat,
                showtime.getBasePrice()
        ));
        return savedBooking;
    }

    private Showtime createShowtimeFixture() {
        String suffix = Long.toString(System.nanoTime());
        Movie movie = new Movie("Phase 6 Payment Movie " + suffix, 110, MovieStatus.NOW_SHOWING);
        movie.setDescription("Payment flow movie.");
        movie.setReleaseDate(LocalDate.of(2026, 5, 19));
        movie.setLanguage("English");
        movie.setSubtitleLanguage("Vietnamese");
        movie.setAgeRating(AgeRating.from("13+"));
        movie.setDirector("Payment Director");
        movie.setMainActors("Payment Lead");
        movie.setCastList("Cast");
        Movie savedMovie = movieRepository.save(movie);

        Cinema cinema = cinemaRepository.save(new Cinema("Phase 6 Payment Cinema " + suffix, "1 Payment Street", "HCMC", "0900666777"));
        Room room = roomRepository.save(new Room(cinema, "Payment Room", RoomType.TWO_D, 1, 2));
        SeatRow seatRow = seatRowRepository.save(new SeatRow(room, "A", 1, 1, SeatType.NORMAL));
        seatRepository.save(new Seat(room, seatRow, 1, 1, SeatType.NORMAL));
        seatRepository.save(new Seat(room, seatRow, 2, 2, SeatType.NORMAL));

        Showtime showtime = new Showtime(
                savedMovie,
                room,
                LocalDateTime.of(2026, 5, 21, 19, 0),
                LocalDateTime.of(2026, 5, 21, 21, 5),
                BigDecimal.valueOf(95000)
        );
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

    private User createUser(String email, String password, RoleName roleName) {
        Role role = roleRepository.findByName(roleName)
                .orElseGet(() -> roleRepository.save(new Role(roleName)));

        User user = new User(email, passwordEncoder.encode(password), "Phase Six Payment User", "0900666888");
        user.setEmailVerified(true);
        user.setStatus(com.sba301.cinemaai.enums.UserStatus.ACTIVE);
        User savedUser = userRepository.save(user);
        userRoleRepository.save(new UserRole(savedUser, role));
        return savedUser;
    }
}
