package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.enums.ShowtimeStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ShowtimeRepository extends JpaRepository<Showtime, Long> {

    List<Showtime> findByMovie(Movie movie);

    List<Showtime> findByRoom(Room room);

    List<Showtime> findByStatus(ShowtimeStatus status);

    List<Showtime> findByStartTimeBetween(LocalDateTime from, LocalDateTime to);

    Optional<Showtime> findByRoomAndMovieAndStartTime(Room room, Movie movie, LocalDateTime startTime);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT s FROM Showtime s WHERE s.id = :id")
    Optional<Showtime> findByIdForUpdate(@Param("id") Long id);

    boolean existsByRoomAndMovieAndStartTime(Room room, Movie movie, LocalDateTime startTime);

    /** Single-row lookup with movie/room/cinema pre-fetched — avoids lazy N+1 when mapping to response. */
    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    Optional<Showtime> findWithDetailsById(Long id);

    /**
     * Admin paged search – every filter is optional.
     * When a parameter is null the corresponding WHERE clause is skipped.
     */
    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    @Query("""
            select s from Showtime s
            where (:movieId   is null or s.movie.id             = :movieId)
              and (:roomId    is null or s.room.id              = :roomId)
              and (:cinemaId  is null or s.room.cinema.id       = :cinemaId)
              and (:status    is null or s.status               = :status)
              and s.startTime >= :from
              and s.startTime <  :to
            """)
    Page<Showtime> searchAdmin(
            @Param("movieId")  Long movieId,
            @Param("roomId")   Long roomId,
            @Param("cinemaId") Long cinemaId,
            @Param("status")   ShowtimeStatus status,
            @Param("from")     LocalDateTime from,
            @Param("to")       LocalDateTime to,
            Pageable pageable
    );

    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    @Query("""
            select s from Showtime s
            where (:movieId is null or s.movie.id = :movieId)
              and (:roomId is null or s.room.id = :roomId)
              and s.status = com.cinemaai.catalog.enums.ShowtimeStatus.OPEN
              and s.startTime >= :from
              and s.startTime < :to
            """)
    Page<Showtime> searchPublic(
            @Param("movieId") Long movieId,
            @Param("roomId") Long roomId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to,
            Pageable pageable
    );

    @Query("""
            select count(showtime) > 0
            from Showtime showtime
            where showtime.room = :room
              and showtime.status <> com.cinemaai.catalog.enums.ShowtimeStatus.CANCELLED
              and (:excludeId is null or showtime.id <> :excludeId)
              and showtime.startTime < :endTime
              and showtime.endTime > :startTime
            """)
    boolean existsOverlapping(
            @Param("room") Room room,
            @Param("startTime") LocalDateTime startTime,
            @Param("endTime") LocalDateTime endTime,
            @Param("excludeId") Long excludeId
    );

    @Query("SELECT s FROM Showtime s WHERE s.status = 'SCHEDULED' AND s.startTime <= :now")
    List<Showtime> findScheduledReadyToOpen(@Param("now") LocalDateTime now);

    @Query("SELECT s FROM Showtime s WHERE s.status = 'OPEN' AND s.endTime <= :now")
    List<Showtime> findOpenReadyToComplete(@Param("now") LocalDateTime now);

    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    @Query("""
            select s from Showtime s
            where (:movieId is null or s.movie.id = :movieId)
              and s.status = com.cinemaai.catalog.enums.ShowtimeStatus.OPEN
              and s.room.status = com.cinemaai.catalog.enums.RoomStatus.ACTIVE
              and s.movie.status <> com.cinemaai.catalog.enums.MovieStatus.INACTIVE
              and s.startTime >= :from
              and s.startTime < :to
            order by s.startTime asc, s.id asc
            """)
    List<Showtime> findCustomerCandidateShowtimes(
            @Param("movieId") Long movieId,
            @Param("from") LocalDateTime from,
            @Param("to") LocalDateTime to
    );

    @EntityGraph(attributePaths = {"movie", "room", "room.cinema"})
    @Query("""
            select s from Showtime s
            where s.movie.id = :movieId
              and s.startTime = :startTime
              and s.status = com.cinemaai.catalog.enums.ShowtimeStatus.OPEN
              and s.room.status = com.cinemaai.catalog.enums.RoomStatus.ACTIVE
              and s.movie.status <> com.cinemaai.catalog.enums.MovieStatus.INACTIVE
            order by s.id asc
            """)
    List<Showtime> findEquivalentCandidateShowtimes(
            @Param("movieId") Long movieId,
            @Param("startTime") LocalDateTime startTime
    );
}
