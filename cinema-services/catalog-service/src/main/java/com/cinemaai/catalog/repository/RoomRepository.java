package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Cinema;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.enums.RoomStatus;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RoomRepository extends JpaRepository<Room, Long> {

    Optional<Room> findByCinemaAndNameIgnoreCase(Cinema cinema, String name);

    List<Room> findByCinema(Cinema cinema);

    List<Room> findByStatus(RoomStatus status);
}
