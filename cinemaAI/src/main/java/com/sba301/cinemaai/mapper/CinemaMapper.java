package com.sba301.cinemaai.mapper;

import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.dto.response.cinema.RoomResponse;
import com.sba301.cinemaai.dto.response.cinema.SeatResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatResponse;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Showtime;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.springframework.stereotype.Component;

@Component
public class CinemaMapper {

    public CinemaResponse toCinemaResponse(Cinema cinema) {
        return new CinemaResponse(
                cinema.getId(),
                cinema.getName(),
                cinema.getAddress(),
                cinema.getCity(),
                cinema.getPhone(),
                cinema.getStatus(),
                cinema.getCreatedAt(),
                cinema.getUpdatedAt()
        );
    }

    public RoomResponse toRoomResponse(Room room) {
        return new RoomResponse(
                room.getId(),
                room.getCinema().getId(),
                room.getCinema().getName(),
                room.getName(),
                room.getRoomType(),
                room.getRowCount(),
                room.getColumnCount(),
                room.getStatus(),
                room.getCreatedAt(),
                room.getUpdatedAt()
        );
    }

    public SeatResponse toSeatResponse(Seat seat) {
        return new SeatResponse(
                seat.getId(),
                seat.getRoom().getId(),
                seat.getSeatRow().getId(),
                seat.getRowLabel(),
                seat.getSeatRow().getDisplayOrder(),
                seat.getSeatNumber(),
                seat.getDisplayColumn(),
                seat.getSeatRow().getStartColumn(),
                seat.getSeatType(),
                seat.getStatus()
        );
    }

    /** Maps a Showtime to ShowtimeResponse without genre info (backward compat). */
    public ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return toShowtimeResponse(showtime, Collections.emptyList());
    }

    /** Maps a Showtime to ShowtimeResponse with pre-fetched genre names. */
    public ShowtimeResponse toShowtimeResponse(Showtime showtime, List<String> genreNames) {
        com.sba301.cinemaai.entity.Movie movie = showtime.getMovie();
        String ageRating = movie.getAgeRating() != null ? movie.getAgeRating().name() : null;

        return new ShowtimeResponse(
                showtime.getId(),
                movie.getId(),
                movie.getTitle(),
                ageRating,
                genreNames != null ? genreNames : Collections.emptyList(),
                showtime.getRoom().getCinema().getId(),
                showtime.getRoom().getCinema().getName(),
                showtime.getRoom().getId(),
                showtime.getRoom().getName(),
                showtime.getStartTime(),
                showtime.getEndTime(),
                showtime.getBasePrice(),
                showtime.getVipPrice(),
                showtime.getCouplePrice(),
                showtime.getAdultStandardPrice(),
                showtime.getChildStandardPrice(),
                showtime.getStudentStandardPrice(),
                showtime.getAdultVipPrice(),
                showtime.getChildVipPrice(),
                showtime.getStudentVipPrice(),
                showtime.getAdultCouplePrice(),
                showtime.getChildCouplePrice(),
                showtime.getStudentCouplePrice(),
                showtime.isWeekendSurcharge(),
                showtime.isHolidaySurcharge(),
                showtime.getLateNightSurchargeAmount(),
                showtime.getSurchargeAmount(),
                showtime.getStatus(),
                showtime.getCancellationReason(),
                showtime.getCancelledAt(),
                showtime.getCreatedAt(),
                showtime.getUpdatedAt()
        );
    }

    public ShowtimeSeatResponse toShowtimeSeatResponse(Seat seat, String runtimeStatus, LocalDateTime holdExpiresAt, Showtime showtime) {
        return new ShowtimeSeatResponse(
                seat.getId(),
                seat.getSeatRow().getId(),
                seat.getRowLabel(),
                seat.getSeatRow().getDisplayOrder(),
                seat.getSeatNumber(),
                seat.getDisplayColumn(),
                seat.getSeatRow().getStartColumn(),
                seat.getSeatType(),
                seat.getStatus(),
                runtimeStatus,
                holdExpiresAt,
                showtime.getPriceForSeatType(seat.getSeatType())
        );
    }
}
