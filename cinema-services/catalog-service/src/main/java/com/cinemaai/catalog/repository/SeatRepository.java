package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoom(Room room);

    List<Seat> findByRoomInAndStatus(List<Room> rooms, com.cinemaai.catalog.enums.SeatStatus status);

    Optional<Seat> findByRoomAndRowLabelAndSeatNumber(Room room, String rowLabel, int seatNumber);

    boolean existsByRoomAndRowLabelAndSeatNumber(Room room, String rowLabel, int seatNumber);

    @Modifying
    @Query("delete from Seat seat where seat.room = :room")
    void deleteByRoom(@Param("room") Room room);
}
