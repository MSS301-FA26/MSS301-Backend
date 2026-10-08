package com.cinemaai.catalog.repository;

import com.cinemaai.catalog.entity.TicketPricingRule;
import com.cinemaai.catalog.enums.RoomType;
import com.cinemaai.catalog.enums.SeatType;
import com.cinemaai.catalog.enums.TicketType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface TicketPricingRuleRepository extends JpaRepository<TicketPricingRule, Long> {

    List<TicketPricingRule> findByActiveTrue();

    Optional<TicketPricingRule> findFirstByCinemaIdAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueOrderByUpdatedAtDesc(
            Long cinemaId,
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    Optional<TicketPricingRule> findFirstByCinemaIdIsNullAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueOrderByUpdatedAtDesc(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    Optional<TicketPricingRule> findFirstByTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueOrderByUpdatedAtDesc(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    boolean existsByTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrue(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    boolean existsByTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueAndIdNot(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            Long id
    );

    boolean existsByCinemaIdAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrue(
            Long cinemaId,
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    boolean existsByCinemaIdIsNullAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrue(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday
    );

    boolean existsByCinemaIdAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueAndIdNot(
            Long cinemaId,
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            Long id
    );

    boolean existsByCinemaIdIsNullAndTicketTypeAndRoomTypeAndSeatTypeAndWeekendAndHolidayAndActiveTrueAndIdNot(
            TicketType ticketType,
            RoomType roomType,
            SeatType seatType,
            boolean weekend,
            boolean holiday,
            Long id
    );

    @Query("""
            select rule from TicketPricingRule rule
            where (:cinemaId is null or rule.cinemaId = :cinemaId)
              and (:ticketType is null or rule.ticketType = :ticketType)
              and (:roomType is null or rule.roomType = :roomType)
              and (:seatType is null or rule.seatType = :seatType)
              and (:active is null or rule.active = :active)
            """)
    Page<TicketPricingRule> searchAdmin(
            @Param("cinemaId") Long cinemaId,
            @Param("ticketType") TicketType ticketType,
            @Param("roomType") RoomType roomType,
            @Param("seatType") SeatType seatType,
            @Param("active") Boolean active,
            Pageable pageable
    );
}
