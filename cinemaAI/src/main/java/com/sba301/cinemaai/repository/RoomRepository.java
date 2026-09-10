package com.sba301.cinemaai.repository;

import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.enums.RoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCinemaAndNameIgnoreCase(Cinema cinema, String name);

    List<Room> findByCinema(Cinema cinema);

    List<Room> findByStatus(RoomStatus status);
}
