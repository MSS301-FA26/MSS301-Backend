package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRepository extends JpaRepository<Seat, Long> {

    List<Seat> findByRoom(Room room);

    Optional<Seat> findByRoomAndRowLabelAndSeatNumber(Room room, String rowLabel, int seatNumber);

    boolean existsByRoomAndRowLabelAndSeatNumber(Room room, String rowLabel, int seatNumber);

    @Modifying
    @Query("delete from Seat seat where seat.room = :room")
    void deleteByRoom(@Param("room") Room room);
}
