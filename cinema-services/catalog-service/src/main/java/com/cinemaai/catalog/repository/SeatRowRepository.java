package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.SeatRow;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SeatRowRepository extends JpaRepository<SeatRow, Long> {

    List<SeatRow> findByRoom(Room room);

    Optional<SeatRow> findByRoomAndRowLabel(Room room, String rowLabel);

    @Modifying
    @Query("delete from SeatRow seatRow where seatRow.room = :room")
    void deleteByRoom(@Param("room") Room room);
}
