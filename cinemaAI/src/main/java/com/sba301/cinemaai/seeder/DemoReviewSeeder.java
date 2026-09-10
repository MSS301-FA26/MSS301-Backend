package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Review;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.BookingStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.ReviewRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Seeds Vietnamese demo customers with real booking-linked reviews so admin can
 * audit each review by movie, showtime, cinema and room.
 * Password for every demo account: Demo@123
 */
@Component
@Order(50)
@RequiredArgsConstructor
public class DemoReviewSeeder implements Seeder {

    private static final String DEMO_PASSWORD = "Demo@123";

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final MovieRepository movieRepository;
    private final ShowtimeRepository showtimeRepository;
    private final BookingRepository bookingRepository;
    private final ReviewRepository reviewRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void seed() {
        seedPersona("demo.scifi@cinemaai.com", "Nguyễn Minh Khoa", "0911000001", Map.of(
                "Interstellar", r(5, "Tuyệt tác! Xem xong vẫn còn ngồi lặng thêm mấy phút."),
                "Inception", r(5, "Kịch bản rất chắc tay, càng nghĩ càng thấy cuốn."),
                "The Dark Knight", r(5, "Joker quá ám ảnh, phòng chiếu im phăng phắc ở đoạn cao trào."),
                "Avengers: Endgame", r(4, "Cái kết trọn vẹn cho một hành trình dài của Marvel."),
                "Spider-Man: Into the Spider-Verse", r(4, "Hình ảnh sáng tạo, nhịp phim trẻ trung và rất đã mắt."),
                "The Grand Budapest Hotel", r(3, "Màu phim đẹp nhưng không hẳn là gu của mình.")
        ));
        seedPersona("demo.romance@cinemaai.com", "Trần Thảo Linh", "0911000002", Map.of(
                "The Green Mile", r(5, "Câu chuyện rất nhân văn, ra khỏi rạp vẫn thấy nghẹn."),
                "The Grand Budapest Hotel", r(4, "Màu sắc, âm nhạc và thoại đều duyên."),
                "Parasite", r(4, "Ám ảnh nhưng cực kỳ đáng xem, dựng cảnh rất tinh tế."),
                "The Dark Knight", r(2, "Phim hay nhưng hơi nặng và không hợp tâm trạng của mình."),
                "Interstellar", r(3, "Hơi dài, bù lại đoạn cuối rất cảm xúc.")
        ));
        seedPersona("demo.action@cinemaai.com", "Lê Hoàng Đức", "0911000003", Map.of(
                "The Dark Knight", r(5, "Một trong những phim siêu anh hùng hay nhất mình từng xem."),
                "Avengers: Endgame", r(5, "Trận chiến cuối quá mãn nhãn, cả rạp vỗ tay."),
                "Black Panther", r(4, "Không khí Wakanda rất cuốn, hành động ổn."),
                "Inception", r(4, "Vừa hành động vừa xoắn não, xem rất phê."),
                "The Green Mile", r(3, "Phim hay nhưng tiết tấu hơi chậm với mình.")
        ));
        seedPersona("demo.drama@cinemaai.com", "Phạm Mai Anh", "0911000004", Map.of(
                "Parasite", r(5, "Một phim châu Á xuất sắc, càng xem càng thấm."),
                "The Green Mile", r(5, "Diễn xuất tuyệt vời, câu chuyện để lại dư âm lâu."),
                "The Grand Budapest Hotel", r(5, "Chỉn chu từ khung hình đến nhịp kể chuyện."),
                "Interstellar", r(4, "Khoa học viễn tưởng nhưng phần cảm xúc rất mạnh."),
                "Avengers: Endgame", r(2, "Giải trí ổn, nhưng mình không quá thích kiểu phim này.")
        ));
        seedPersona("demo.family@cinemaai.com", "Võ Gia Tuấn", "0911000005", Map.of(
                "Spider-Man: Into the Spider-Verse", r(5, "Cả nhà xem đều thích, hình ảnh quá sáng tạo."),
                "Avengers: Endgame", r(4, "Đưa con đi xem, không khí trong rạp rất vui."),
                "Black Panther", r(4, "Con mình rất thích phần phục trang và thế giới Wakanda."),
                "The Grand Budapest Hotel", r(3, "Người lớn thấy hay, trẻ nhỏ thì hơi khó theo dõi.")
        ));
        seedPersona("demo.cinephile@cinemaai.com", "Đặng Hà My", "0911000006", Map.of(
                "Parasite", r(4, "Bong Joon-ho kể chuyện quá khéo, nhiều chi tiết đáng suy ngẫm."),
                "Interstellar", r(4, "Nolan và Zimmer là một combo rất đáng tiền vé."),
                "Black Panther", r(3, "Ổn trong mặt bằng Marvel, phần phản diện khá tốt."),
                "Inception", r(5, "Kịch bản chặt chẽ đến từng lớp mơ."),
                "The Dark Knight", r(4, "Xem lại vẫn nổi da gà ở nhiều phân đoạn.")
        ));
    }

    private void seedPersona(String email, String fullName, String phone, Map<String, ReviewSeed> reviews) {
        Role customerRole = roleRepository.findByName(RoleName.CUSTOMER)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.CUSTOMER)));

        User user = userRepository.findByEmail(email)
                .orElseGet(() -> {
                    User created = new User(email, passwordEncoder.encode(DEMO_PASSWORD), fullName, phone);
                    created.setEmailVerified(true);
                    created.setStatus(UserStatus.ACTIVE);
                    return userRepository.save(created);
                });
        syncProfile(user, fullName, phone);

        if (!userRoleRepository.existsByUserIdAndRoleId(user.getId(), customerRole.getId())) {
            userRoleRepository.save(new UserRole(user, customerRole));
        }

        reviews.forEach((movieTitle, seed) ->
                movieRepository.findByTitle(movieTitle).ifPresent(movie -> seedReview(user, movie, seed))
        );
    }

    private void seedReview(User user, Movie movie, ReviewSeed seed) {
        Booking booking = resolveDemoBooking(user, movie);
        if (booking == null) {
            return;
        }

        reviewRepository.findByUserAndMovie(user, movie).ifPresentOrElse(existing -> {
            existing.setBooking(booking);
            existing.setRating(seed.rating());
            existing.setComment(seed.comment());
        }, () -> reviewRepository.save(new Review(user, movie, booking, seed.rating(), seed.comment())));
    }

    private Booking resolveDemoBooking(User user, Movie movie) {
        var usedBookings = bookingRepository.findUsedBookingsByUserAndMovieOrderByLatest(user, movie);
        if (!usedBookings.isEmpty()) {
            return usedBookings.get(0);
        }

        Showtime showtime = showtimeRepository.findByMovie(movie).stream()
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .findFirst()
                .orElse(null);
        if (showtime == null) {
            return null;
        }

        String bookingCode = "RVU" + user.getId() + "M" + movie.getId();
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .orElseGet(() -> new Booking(bookingCode, user, showtime, showtime.getStartTime().plusMinutes(15)));

        BigDecimal totalAmount = showtime.getAdultStandardPrice() != null
                ? showtime.getAdultStandardPrice()
                : showtime.getBasePrice();
        booking.setSubtotal(totalAmount);
        booking.setDiscountAmount(BigDecimal.ZERO);
        booking.setTotalAmount(totalAmount);
        booking.setStatus(BookingStatus.USED);
        booking.setPaidAt(showtime.getStartTime().minusMinutes(45));
        booking.setCheckedInAt(showtime.getStartTime().minusMinutes(10));
        booking.setQrCode("DEMO-REVIEW-" + bookingCode);
        return bookingRepository.save(booking);
    }

    private void syncProfile(User user, String fullName, String phone) {
        if (user.getProfile() == null) {
            return;
        }
        user.getProfile().setFullName(fullName);
        user.getProfile().setPhone(phone);
        user.setEmailVerified(true);
        user.setStatus(UserStatus.ACTIVE);
    }

    private static ReviewSeed r(int rating, String comment) {
        return new ReviewSeed(rating, comment);
    }

    private record ReviewSeed(int rating, String comment) {
    }
}
