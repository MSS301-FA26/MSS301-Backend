package com.cinemaai.catalog.mapper;

import com.cinemaai.catalog.dto.response.cinema.CinemaResponse;
import com.cinemaai.catalog.dto.response.cinema.RoomResponse;
import com.cinemaai.catalog.dto.response.cinema.SeatResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatResponse;
import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Seat;
import com.cinemaai.catalog.entity.Showtime;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public abstract class CinemaMapper {

    public abstract CinemaResponse toCinemaResponse(Cinema cinema);

    @Mapping(target = "cinemaId", source = "cinema.id")
    @Mapping(target = "cinemaName", source = "cinema.name")
    public abstract RoomResponse toRoomResponse(Room room);

    @Mapping(target = "roomId", source = "room.id")
    @Mapping(target = "seatRowId", source = "seatRow.id")
    @Mapping(target = "displayOrder", source = "seatRow.displayOrder")
    @Mapping(target = "startColumn", source = "seatRow.startColumn")
    public abstract SeatResponse toSeatResponse(Seat seat);

    /** Maps a Showtime to ShowtimeResponse without genre info (backward compat). */
    public ShowtimeResponse toShowtimeResponse(Showtime showtime) {
        return toShowtimeResponse(showtime, Collections.emptyList());
    }

    @Mapping(target = "id", source = "showtime.id")
    @Mapping(target = "movieId", source = "showtime.movie.id")
    @Mapping(target = "movieTitle", source = "showtime.movie.title")
    @Mapping(target = "movieAgeRating", expression = "java(showtime.getMovie() != null && showtime.getMovie().getAgeRating() != null ? showtime.getMovie().getAgeRating().name() : null)")
    @Mapping(target = "movieGenreNames", expression = "java(genreNames != null ? genreNames : java.util.Collections.emptyList())")
    @Mapping(target = "cinemaId", source = "showtime.room.cinema.id")
    @Mapping(target = "cinemaName", source = "showtime.room.cinema.name")
    @Mapping(target = "roomId", source = "showtime.room.id")
    @Mapping(target = "roomName", source = "showtime.room.name")
    @Mapping(target = "status", source = "showtime.status")
    @Mapping(target = "createdAt", source = "showtime.createdAt")
    @Mapping(target = "updatedAt", source = "showtime.updatedAt")
    public abstract ShowtimeResponse toShowtimeResponse(Showtime showtime, List<String> genreNames);

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
