package com.sba301.cinemaai.seeder;

import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.SeatRow;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.RoomType;
import com.sba301.cinemaai.enums.SeatType;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.MovieRepository;
import com.sba301.cinemaai.repository.RoomRepository;
import com.sba301.cinemaai.repository.SeatRepository;
import com.sba301.cinemaai.repository.SeatRowRepository;
import com.sba301.cinemaai.repository.ShowtimeRepository;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.core.annotation.Order;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Order(40)
@RequiredArgsConstructor
public class CinemaScheduleSeeder implements Seeder {

    private final CinemaRepository cinemaRepository;
    private final RoomRepository roomRepository;
    private final SeatRepository seatRepository;
    private final SeatRowRepository seatRowRepository;
    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;

    private static final BigDecimal ADULT_STANDARD_PRICE = BigDecimal.valueOf(90_000);
    private static final BigDecimal CHILD_STANDARD_PRICE = BigDecimal.valueOf(70_000);
    private static final BigDecimal STUDENT_STANDARD_PRICE = BigDecimal.valueOf(75_000);
    private static final BigDecimal ADULT_VIP_PRICE = BigDecimal.valueOf(120_000);
    private static final BigDecimal CHILD_VIP_PRICE = BigDecimal.valueOf(95_000);
    private static final BigDecimal STUDENT_VIP_PRICE = BigDecimal.valueOf(100_000);
    private static final BigDecimal ADULT_COUPLE_PRICE = BigDecimal.valueOf(180_000);
    private static final BigDecimal CHILD_COUPLE_PRICE = BigDecimal.valueOf(140_000);
    private static final BigDecimal STUDENT_COUPLE_PRICE = BigDecimal.valueOf(150_000);

    @Override
    @Transactional
    public void seed() {
        Cinema cinema = cinemaRepository.findByName("CineAI Central")
                .orElseGet(() -> cinemaRepository.save(new Cinema(
                        "CineAI Central",
                        "1 Nguyen Hue, District 1",
                        "Ho Chi Minh City",
                        "0900000000"
                )));

        Room roomA = roomRepository.findByCinemaAndNameIgnoreCase(cinema, "Room A")
                .orElseGet(() -> roomRepository.save(new Room(cinema, "Room A", RoomType.TWO_D, 5, 8)));
        Room roomB = roomRepository.findByCinemaAndNameIgnoreCase(cinema, "Room B")
                .orElseGet(() -> roomRepository.save(new Room(cinema, "Room B", RoomType.THREE_D, 5, 8)));
        Room roomC = roomRepository.findByCinemaAndNameIgnoreCase(cinema, "Room C")
                .orElseGet(() -> roomRepository.save(new Room(cinema, "Room C", RoomType.IMAX, 5, 8)));

        seedSeats(roomA);
        seedSeats(roomB);
        seedSeats(roomC);
        seedShowtimes(java.util.List.of(roomA, roomB, roomC));
        backfillMissingTicketPrices();
    }

    private void seedSeats(Room room) {
        for (char row = 'A'; row <= 'E'; row++) {
            String rowLabel = Character.toString(row);
            SeatType rowType = row == 'E' ? SeatType.VIP : SeatType.NORMAL;
            int displayOrder = row - 'A' + 1;
            SeatRow seatRow = seatRowRepository.findByRoomAndRowLabel(room, rowLabel)
                    .orElseGet(() -> seatRowRepository.save(new SeatRow(
                            room,
                            rowLabel,
                            displayOrder,
                            1,
                            rowType
                    )));
            for (int number = 1; number <= 8; number++) {
                if (!seatRepository.existsByRoomAndRowLabelAndSeatNumber(room, rowLabel, number)) {
                    seatRepository.save(new Seat(room, seatRow, number, number, rowType));
                }
            }
        }
    }

    private void seedShowtimes(java.util.List<Room> rooms) {
        java.util.List<Movie> nowShowing = movieRepository.findAll().stream()
                .filter(movie -> movie.getStatus() == com.sba301.cinemaai.enums.MovieStatus.NOW_SHOWING)
                .toList();
        if (nowShowing.isEmpty()) {
            return;
        }

        // 4 fixed daily slots per room; slots are >200 minutes apart so even the longest movie never overlaps.
        int[][] slots = {{9, 30}, {13, 0}, {16, 30}, {20, 30}};
        int movieIndex = 0;
        for (int day = 1; day <= 5; day++) {
            for (Room room : rooms) {
                for (int[] slot : slots) {
                    Movie movie = nowShowing.get(movieIndex % nowShowing.size());
                    movieIndex++;
                    LocalDateTime startTime = LocalDateTime.now()
                            .plusDays(day).withHour(slot[0]).withMinute(slot[1]).withSecond(0).withNano(0);
                    seedShowtime(room, movie, startTime);
                }
            }
        }
    }

    private void seedShowtime(Room room, Movie movie, LocalDateTime startTime) {
        var existing = showtimeRepository.findByRoomAndMovieAndStartTime(room, movie, startTime);
        if (existing.isPresent()) {
            applyDefaultTicketPrices(existing.get());
            return;
        }

        LocalDateTime endTime = startTime.plusMinutes(movie.getDurationMinutes()).plusMinutes(15);
        Showtime showtime = new Showtime(movie, room, startTime, endTime, ADULT_STANDARD_PRICE);
        applyDefaultTicketPrices(showtime);
        showtime.setStatus(ShowtimeStatus.OPEN);
        showtimeRepository.save(showtime);
    }

    private void applyDefaultTicketPrices(Showtime showtime) {
        if (showtime.getVipPrice() == null) {
            showtime.setVipPrice(ADULT_VIP_PRICE);
        }
        if (showtime.getCouplePrice() == null) {
            showtime.setCouplePrice(ADULT_COUPLE_PRICE);
        }
        if (showtime.getAdultStandardPrice() == null) {
            showtime.setAdultStandardPrice(ADULT_STANDARD_PRICE);
        }
        if (showtime.getChildStandardPrice() == null) {
            showtime.setChildStandardPrice(CHILD_STANDARD_PRICE);
        }
        if (showtime.getStudentStandardPrice() == null) {
            showtime.setStudentStandardPrice(STUDENT_STANDARD_PRICE);
        }
        if (showtime.getAdultVipPrice() == null) {
            showtime.setAdultVipPrice(ADULT_VIP_PRICE);
        }
        if (showtime.getChildVipPrice() == null) {
            showtime.setChildVipPrice(CHILD_VIP_PRICE);
        }
        if (showtime.getStudentVipPrice() == null) {
            showtime.setStudentVipPrice(STUDENT_VIP_PRICE);
        }
        if (showtime.getAdultCouplePrice() == null) {
            showtime.setAdultCouplePrice(ADULT_COUPLE_PRICE);
        }
        if (showtime.getChildCouplePrice() == null) {
            showtime.setChildCouplePrice(CHILD_COUPLE_PRICE);
        }
        if (showtime.getStudentCouplePrice() == null) {
            showtime.setStudentCouplePrice(STUDENT_COUPLE_PRICE);
        }
    }

    private void backfillMissingTicketPrices() {
        showtimeRepository.findAll().forEach(this::applyDefaultTicketPrices);
    }
}
