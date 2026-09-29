package com.cinemaai.catalog.service.impl;

import com.cinemaai.catalog.dto.request.cinema.BulkShowtimeRequest;
import com.cinemaai.catalog.dto.request.cinema.ShowtimeRequest;
import com.cinemaai.catalog.dto.request.cinema.ShowtimePreviewRequest;
import com.cinemaai.catalog.dto.response.cinema.ShowtimePricePreviewResponse;
import com.cinemaai.catalog.entity.CinemaAudiencePrice;
import com.cinemaai.catalog.enums.AudienceType;
import com.cinemaai.catalog.repository.CinemaAudiencePriceRepository;
import com.cinemaai.catalog.dto.response.PageResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatMapResponse;
import com.cinemaai.catalog.dto.response.cinema.ShowtimeSeatResponse;
import com.cinemaai.catalog.entity.Movie;
import com.cinemaai.catalog.entity.Room;
import com.cinemaai.catalog.entity.Seat;
import com.cinemaai.catalog.entity.Showtime;
import com.cinemaai.catalog.enums.*;
import com.cinemaai.catalog.exception.BadRequestException;
import com.cinemaai.catalog.exception.ConflictException;
import com.cinemaai.catalog.exception.NotFoundException;
import com.cinemaai.catalog.mapper.CinemaMapper;
import com.cinemaai.catalog.repository.*;
import com.cinemaai.catalog.dto.response.cinema.AvailableSlotResponse;
import com.cinemaai.catalog.dto.response.cinema.CustomerShowtimeSlotResponse;
import com.cinemaai.catalog.service.AuditLogService;
import com.cinemaai.catalog.service.RoomService;
import com.cinemaai.catalog.service.ShowtimeService;
import java.time.LocalTime;
import java.util.LinkedHashMap;
import java.util.ArrayList;
import java.util.HashMap;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
public class ShowtimeServiceImpl implements ShowtimeService {

    private static final int CLEANUP_MINUTES = 15;
    // Giờ hoạt động của rạp (một rạp duy nhất) dùng cho gợi ý khung giờ trống
    private static final LocalTime OPERATING_START = LocalTime.of(8, 0);
    private static final LocalTime OPERATING_END = LocalTime.of(23, 59);
    private static final int SLOT_STEP_MINUTES = 15;
    private static final int MAX_SUGGESTED_SLOTS = 24;

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final RoomService roomService;
    private final CinemaMapper cinemaMapper;
    private final AuditLogService auditLogService;
    private final com.cinemaai.catalog.client.BookingClient bookingClient;
    private final CinemaAudiencePriceRepository audiencePriceRepository;


    // -------------------------------------------------------------------------
    // PUBLIC (customer-facing)
    // -------------------------------------------------------------------------

    /**
     * Public showtime search: only returns OPEN showtimes.
     * SCHEDULED is an internal admin state — customers cannot book it.
     */
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> searchPublic(Long movieId, Long roomId, Long cinemaId, LocalDate date, int page, int size) {
        LocalDateTime from = date == null ? LocalDate.now().atStartOfDay() : date.atStartOfDay();
        LocalDateTime to = date == null ? LocalDate.now().plusYears(1).atStartOfDay() : date.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        if (from.isBefore(now)) {
            from = now;
        }

        org.springframework.data.domain.Page<Showtime> showtimePage =
                showtimeRepository.searchPublic(movieId, roomId, cinemaId, from, to,
                        pageable(page, size, Sort.by("startTime").ascending()));

        // Batch-fetch genres for all movies on this page
        List<Long> movieIds = showtimePage.stream()
                .map(st -> st.getMovie().getId())
                .distinct()
                .collect(Collectors.toList());
        Map<Long, List<String>> genresByMovieId = movieIds.isEmpty() ? Map.of()
                : movieGenreRepository.findWithGenreByMovieIdIn(movieIds).stream()
                    .filter(mg -> mg.getGenre() != null)
                    .collect(Collectors.groupingBy(
                        mg -> mg.getMovie().getId(),
                        Collectors.mapping(mg -> mg.getGenre().getName(), Collectors.toList())
                    ));

        return PageResponse.from(showtimePage.map(st ->
                cinemaMapper.toShowtimeResponse(st,
                        genresByMovieId.getOrDefault(st.getMovie().getId(), Collections.emptyList()))
        ));
    }

    // -------------------------------------------------------------------------
    // ADMIN
    // -------------------------------------------------------------------------

    /**
     * Admin paged search — all statuses visible, full filter support.
     */
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> searchAdmin(
            Long movieId, Long roomId, Long cinemaId,
            ShowtimeStatus status,
            LocalDate date,
            int page, int size) {

        LocalDateTime from = date == null ? LocalDateTime.of(2000, 1, 1, 0, 0) : date.atStartOfDay();
        LocalDateTime to   = date == null ? LocalDateTime.of(2099, 12, 31, 23, 59) : date.plusDays(1).atStartOfDay();

        Pageable pageable = pageable(page, size, Sort.by("id").descending());
        return PageResponse.from(
                showtimeRepository.searchAdmin(movieId, roomId, cinemaId, status, from, to, pageable)
                        .map(cinemaMapper::toShowtimeResponse)
        );
    }

    /** Admin-only detail view (all statuses visible). */
    @Transactional(readOnly = true)
    public ShowtimeResponse getAdmin(Long id) {
        return cinemaMapper.toShowtimeResponse(findById(id));
    }

    // -------------------------------------------------------------------------
    // SHARED (used by both public and admin controllers)
    // -------------------------------------------------------------------------

    @Transactional(readOnly = true)
    public ShowtimeResponse get(Long id) {
        Showtime showtime = findById(id);
        if ((showtime.getStatus() != ShowtimeStatus.OPEN && showtime.getStatus() != ShowtimeStatus.SCHEDULED)
                || showtime.getMovie().getStatus() == MovieStatus.INACTIVE) {
            throw new NotFoundException("Showtime not found");
        }
        return cinemaMapper.toShowtimeResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse create(ShowtimeRequest request) {
        Movie movie = findMovie(request.movieId());
        Room room = roomService.findById(request.roomId());
        LocalDateTime endTime = calculateEndTime(movie, request.startTime());
        validateShowtime(movie, request.startTime(), room, endTime, null);
        validateChildTicketPricingAllowed(movie, request.childStandardPrice(), request.childVipPrice(), request.childCouplePrice());
        validateInitialStatus(request.status());

        Showtime showtime = new Showtime(movie, room, request.startTime(), endTime, request.basePrice());
        applyPricing(showtime, request);
        showtime.setStatus(request.status() == null ? ShowtimeStatus.SCHEDULED : request.status());
        Showtime saved = showtimeRepository.save(showtime);
        auditLogService.record(AuditActionType.CREATE, "SHOWTIME", saved.getId(),
                movie.getTitle() + " @ " + saved.getStartTime());
        return cinemaMapper.toShowtimeResponse(saved);
    }

    /**
     * Gợi ý các khung giờ trống của một phòng trong ngày cho phim đã chọn.
     * Duyệt các khoảng trống giữa những suất hiện có (không CANCELLED),
     * đề xuất giờ bắt đầu theo bước 15 phút trong giờ hoạt động của rạp.
     */
    @Transactional(readOnly = true)
    public List<AvailableSlotResponse> getAvailableSlots(Long roomId, Long movieId, LocalDate date) {
        Movie movie = findMovie(movieId);
        Room room = roomService.findById(roomId);
        int slotMinutes = movie.getDurationMinutes() + CLEANUP_MINUTES;

        LocalDateTime windowStart = date.atTime(OPERATING_START);
        LocalDateTime windowEnd = date.atTime(OPERATING_END);
        LocalDateTime now = LocalDateTime.now().withSecond(0).withNano(0);
        if (windowStart.isBefore(now)) {
            int remainder = now.getMinute() % SLOT_STEP_MINUTES;
            windowStart = remainder == 0 ? now : now.plusMinutes(SLOT_STEP_MINUTES - remainder);
        }
        if (movie.getReleaseDate() != null && date.isBefore(movie.getReleaseDate())) {
            return List.of();
        }
        if (movie.getEndDate() != null && date.isAfter(movie.getEndDate())) {
            return List.of();
        }
        if (!windowStart.plusMinutes(slotMinutes).isBefore(windowEnd)) {
            return List.of();
        }

        List<Showtime> existing = showtimeRepository.findByStartTimeBetween(
                        date.atStartOfDay(), date.plusDays(1).atStartOfDay())
                .stream()
                .filter(st -> st.getRoom().getId().equals(roomId))
                .filter(st -> st.getStatus() != ShowtimeStatus.CANCELLED)
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .toList();

        List<AvailableSlotResponse> slots = new java.util.ArrayList<>();
        LocalDateTime cursor = windowStart;
        while (slots.size() < MAX_SUGGESTED_SLOTS && cursor.plusMinutes(slotMinutes).isBefore(windowEnd)) {
            LocalDateTime candidateStart = cursor;
            LocalDateTime candidateEnd = cursor.plusMinutes(slotMinutes);
            Showtime conflict = existing.stream()
                    .filter(st -> st.getStartTime().isBefore(candidateEnd)
                            && (st.getEndTime() == null || st.getEndTime().isAfter(candidateStart)))
                    .findFirst()
                    .orElse(null);
            if (conflict == null) {
                slots.add(new AvailableSlotResponse(candidateStart, candidateEnd));
                cursor = cursor.plusMinutes(SLOT_STEP_MINUTES);
            } else {
                // Nhảy tới sau suất đang chiếm chỗ, làm tròn lên bước 15 phút
                LocalDateTime next = (conflict.getEndTime() != null ? conflict.getEndTime() : candidateEnd)
                        .withSecond(0).withNano(0);
                int remainder = next.getMinute() % SLOT_STEP_MINUTES;
                cursor = remainder == 0 ? next : next.plusMinutes(SLOT_STEP_MINUTES - remainder);
            }
        }
        return slots;
    }

    /**
     * Bulk showtime creation — all slots run inside one transaction.
     * If any slot fails validation the whole batch is rolled back.
     * Movie-level validation (INACTIVE check) is done once up front.
     * Each slot independently validates room status, time in future, and overlap.
     */
    @Transactional
    public List<ShowtimeResponse> createBulk(BulkShowtimeRequest request) {
        Movie movie = findMovie(request.movieId());
        if (movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BadRequestException("Cannot schedule inactive movie");
        }
        validateChildTicketPricingAllowed(movie, request.childStandardPrice(), request.childVipPrice(), request.childCouplePrice());
        validateInitialStatus(request.defaultStatus());

        ShowtimeStatus fallbackStatus = request.defaultStatus() == null
                ? ShowtimeStatus.SCHEDULED
                : request.defaultStatus();

        List<ShowtimeResponse> results = new java.util.ArrayList<>();

        Set<String> roomStartTimesInRequest = new HashSet<>();
        for (int i = 0; i < request.slots().size(); i++) {
            BulkShowtimeRequest.Slot slot = request.slots().get(i);
            String slotLabel = "Slot " + (i + 1);
            Room room = roomService.findById(slot.roomId());

            if (room.getStatus() != RoomStatus.ACTIVE) {
                throw new BadRequestException(slotLabel + ": room " + room.getName() + " is not active");
            }
            if (!slot.startTime().isAfter(LocalDateTime.now())) {
                throw new BadRequestException(slotLabel + ": start time must be in the future");
            }
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            if (slot.startTime().toLocalDate().isBefore(tomorrow)) {
                throw new BadRequestException(slotLabel + ": Showtime must be scheduled at least 1 day in advance (from tomorrow onwards)");
            }
            validateShowtimeWithinMovieReleaseWindow(movie, slot.startTime(), slotLabel + ": ");
            if (!roomStartTimesInRequest.add(slot.roomId() + "|" + slot.startTime())) {
                throw new ConflictException(slotLabel + ": room " + room.getName()
                        + " already has another selected slot at " + slot.startTime());
            }
            LocalDateTime endTime = calculateEndTime(movie, slot.startTime());
            if (hasOverlappingShowtime(room, slot.startTime(), endTime, null)) {
                throw new ConflictException(slotLabel + ": room " + room.getName()
                        + " already has an overlapping showtime at " + slot.startTime());
            }

            ShowtimeStatus slotStatus = slot.status() != null ? slot.status() : fallbackStatus;
            validateInitialStatus(slotStatus);

            Showtime showtime = new Showtime(movie, room, slot.startTime(), endTime, request.basePrice());
            applyPricing(showtime, request);
            showtime.setStatus(slotStatus);
            results.add(cinemaMapper.toShowtimeResponse(showtimeRepository.save(showtime)));
        }

        return results;
    }

    @Transactional
    public ShowtimeResponse update(Long id, ShowtimeRequest request) {
        Showtime showtime = findById(id);
        validateShowtimeCanBeUpdated(showtime);
        Movie movie = findMovie(request.movieId());
        Room room = roomService.findById(request.roomId());
        LocalDateTime endTime = calculateEndTime(movie, request.startTime());
        validateShowtime(movie, request.startTime(), room, endTime, id);
        validateChildTicketPricingAllowed(movie, request.childStandardPrice(), request.childVipPrice(), request.childCouplePrice());
        ShowtimeStatus requestedStatus = request.status() == null ? showtime.getStatus() : request.status();
        validateStatusTransition(showtime, requestedStatus);

        showtime.setMovie(movie);
        showtime.setRoom(room);
        showtime.setStartTime(request.startTime());
        showtime.setEndTime(endTime);
        applyPricing(showtime, request);
        showtime.setStatus(requestedStatus);
        auditLogService.record(AuditActionType.UPDATE, "SHOWTIME", showtime.getId(),
                movie.getTitle() + " @ " + showtime.getStartTime());
        return cinemaMapper.toShowtimeResponse(showtime);
    }

    @Transactional
    public ShowtimeResponse updateStatus(Long id, ShowtimeStatus status) {
        Showtime showtime = findById(id);
        validateStatusTransition(showtime, status);
        if (status == ShowtimeStatus.CANCELLED) {
            applyShowtimeCancellation(showtime, null);
        }
        showtime.setStatus(status);
        auditLogService.record(AuditActionType.UPDATE, "SHOWTIME", showtime.getId(),
                showtime.getMovie().getTitle() + " -> " + status);
        return cinemaMapper.toShowtimeResponse(showtime);
    }

    @Transactional
    @Override
    public ShowtimeResponse cancelShowtime(Long id, String reason) {
        Showtime showtime = findById(id);
        validateStatusTransition(showtime, ShowtimeStatus.CANCELLED);
        applyShowtimeCancellation(showtime, reason);
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        auditLogService.record(AuditActionType.DELETE, "SHOWTIME", showtime.getId(),
                "Cancelled: " + (reason == null ? "" : reason));
        return cinemaMapper.toShowtimeResponse(showtime);
    }

    /**
     * Admin hard-delete: permanently removes the showtime from DB.
     * Rejected with 409 if any active bookings still exist.
     * Recommended flow: PATCH status=CANCELLED first (which auto-handles bookings),
     * then DELETE if a full purge is needed.
     */
    @Transactional
    public void delete(Long id) {
        Showtime showtime = findById(id);
        if (hasActiveBookings(showtime)) {
            throw new ConflictException(
                    "Cannot delete showtime because it has active bookings. Cancel the showtime first.");
        }
        auditLogService.record(AuditActionType.DELETE, "SHOWTIME", showtime.getId(),
                showtime.getMovie().getTitle() + " @ " + showtime.getStartTime());
        showtimeRepository.delete(showtime);
    }

    @Transactional(readOnly = true)
    public ShowtimeSeatMapResponse getSeatMap(Long showtimeId) {
        Showtime showtime = findById(showtimeId);
        List<Seat> seats = seatRepository.findByRoom(showtime.getRoom())
                .stream()
                .sorted(Comparator.comparing(Seat::getRowLabel).thenComparingInt(Seat::getSeatNumber))
                .toList();

        List<com.cinemaai.catalog.client.BookingClient.OccupiedSeat> occupiedSeats =
                bookingClient.getOccupiedSeats(showtimeId);
        Map<Long, com.cinemaai.catalog.client.BookingClient.OccupiedSeat> occupiedMap = occupiedSeats.stream()
                .collect(Collectors.toMap(
                        com.cinemaai.catalog.client.BookingClient.OccupiedSeat::seatId,
                        Function.identity(),
                        (a, b) -> a
                ));

        List<ShowtimeSeatResponse> seatResponses = seats.stream()
                .map(seat -> {
                    com.cinemaai.catalog.client.BookingClient.OccupiedSeat occupied = occupiedMap.get(seat.getId());
                    String runtimeStatus;
                    LocalDateTime holdExpiresAt = null;

                    if (seat.getStatus() != SeatStatus.AVAILABLE) {
                        runtimeStatus = "UNAVAILABLE";
                    } else if (occupied != null) {
                        runtimeStatus = occupied.runtimeStatus();
                        holdExpiresAt = occupied.holdExpiresAt();
                    } else {
                        runtimeStatus = "AVAILABLE";
                    }

                    return cinemaMapper.toShowtimeSeatResponse(seat, runtimeStatus, holdExpiresAt, showtime);
                })
                .toList();

        return new ShowtimeSeatMapResponse(
                cinemaMapper.toShowtimeResponse(showtime),
                showtime.getRoom().getRowCount(),
                showtime.getRoom().getColumnCount(),
                seatResponses
        );
    }

    // -------------------------------------------------------------------------
    // PRIVATE HELPERS
    // -------------------------------------------------------------------------

    private List<Showtime> search(Long movieId, Long roomId, LocalDate date) {
        LocalDateTime from = date == null ? LocalDate.now().atStartOfDay()              : date.atStartOfDay();
        LocalDateTime to   = date == null ? LocalDate.now().plusYears(1).atStartOfDay() : date.plusDays(1).atStartOfDay();
        return showtimeRepository.findByStartTimeBetween(from, to)
                .stream()
                .filter(showtime -> movieId == null || showtime.getMovie().getId().equals(movieId))
                .filter(showtime -> roomId  == null || showtime.getRoom().getId().equals(roomId))
                .sorted(Comparator.comparing(Showtime::getStartTime))
                .toList();
    }

    private Pageable pageable(int page, int size, Sort sort) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.max(1, Math.min(size, 100));
        return PageRequest.of(safePage, safeSize, sort);
    }

    private void validateShowtime(Movie movie, LocalDateTime startTime, Room room,
                                  LocalDateTime endTime, Long excludeId) {
        if (movie.getStatus() == MovieStatus.INACTIVE) {
            throw new BadRequestException("Cannot schedule inactive movie");
        }
        validateShowtimeWithinMovieReleaseWindow(movie, startTime, "");
        if (room.getStatus() != RoomStatus.ACTIVE) {
            throw new BadRequestException("Cannot schedule showtime in room " + room.getName() + " because it is not active");
        }
        if (!startTime.isAfter(LocalDateTime.now())) {
            throw new BadRequestException("Showtime start time must be in the future");
        }
        if (excludeId == null) {
            LocalDate tomorrow = LocalDate.now().plusDays(1);
            if (startTime.toLocalDate().isBefore(tomorrow)) {
                throw new BadRequestException("Showtime must be scheduled at least 1 day in advance (from tomorrow onwards)");
            }
        }
        if (!endTime.isAfter(startTime)) {
            throw new BadRequestException("Showtime end time must be after start time");
        }
        if (hasOverlappingShowtime(room, startTime, endTime, excludeId)) {
            throw new ConflictException("Room " + room.getName() + " already has an overlapping showtime");
        }
    }

    private void validateShowtimeWithinMovieReleaseWindow(Movie movie, LocalDateTime startTime, String label) {
        if (startTime == null) {
            return;
        }
        LocalDate releaseDate = movie.getReleaseDate();
        LocalDate endDate = movie.getEndDate();
        if (releaseDate == null || endDate == null) {
            throw new BadRequestException(label + "movie release window is required for showtime scheduling");
        }
        LocalDate showtimeDate = startTime.toLocalDate();
        if (showtimeDate.isBefore(releaseDate) || showtimeDate.isAfter(endDate)) {
            throw new BadRequestException(label + "showtime date must be within movie release window");
        }
    }

    private boolean hasOverlappingShowtime(Room room, LocalDateTime startTime,
                                           LocalDateTime endTime, Long excludeId) {
        return showtimeRepository.existsOverlapping(room, startTime, endTime, excludeId);
    }

    private void validateChildTicketPricingAllowed(
            Movie movie,
            java.math.BigDecimal childStandardPrice,
            java.math.BigDecimal childVipPrice,
            java.math.BigDecimal childCouplePrice
    ) {
        if (movie.getAgeRating() == null || movie.getAgeRating().getMinimumAge() < 16) {
            return;
        }
        if (childStandardPrice != null || childVipPrice != null || childCouplePrice != null) {
            throw new BadRequestException("Child tickets are not allowed for movies rated 16+ or higher");
        }
    }

    private void validateInitialStatus(ShowtimeStatus status) {
        if (status == ShowtimeStatus.CANCELLED || status == ShowtimeStatus.COMPLETED) {
            throw new BadRequestException("New showtime status must be SCHEDULED or OPEN");
        }
    }

    private void validateShowtimeCanBeUpdated(Showtime showtime) {
        if (showtime.getStatus() == ShowtimeStatus.CANCELLED) {
            throw new BadRequestException("Cannot update a cancelled showtime");
        }
        if (showtime.getStatus() == ShowtimeStatus.COMPLETED) {
            throw new BadRequestException("Cannot update a completed showtime");
        }
        if (hasActiveBookings(showtime)) {
            throw new ConflictException("Cannot update showtime because it has active bookings");
        }
    }

    private void validateStatusTransition(Showtime showtime, ShowtimeStatus requestedStatus) {
        if (requestedStatus == null) {
            throw new BadRequestException("Showtime status is required");
        }
        ShowtimeStatus currentStatus = showtime.getStatus();
        if (requestedStatus == ShowtimeStatus.CANCELLED && currentStatus == ShowtimeStatus.OPEN) {
            throw new ConflictException("Published showtime cancellation requires Booking refund coordination");
        }
        if (currentStatus == requestedStatus) {
            return; // no-op
        }
        // Terminal states — nothing can leave them
        if (currentStatus == ShowtimeStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a cancelled showtime");
        }
        if (currentStatus == ShowtimeStatus.COMPLETED && requestedStatus != ShowtimeStatus.CANCELLED) {
            throw new BadRequestException("Cannot change status of a completed showtime");
        }
        // Guard: cannot mark completed before the show ends
        if (requestedStatus == ShowtimeStatus.COMPLETED
                && LocalDateTime.now().isBefore(showtime.getEndTime())) {
            throw new BadRequestException("Cannot complete a showtime before it has ended");
        }
        // Guard: SCHEDULED can only go to OPEN or CANCELLED
        if (currentStatus == ShowtimeStatus.SCHEDULED
                && requestedStatus != ShowtimeStatus.OPEN
                && requestedStatus != ShowtimeStatus.CANCELLED) {
            throw new BadRequestException(
                    "SCHEDULED showtime can only transition to OPEN or CANCELLED");
        }
        // Guard: OPEN can only go to COMPLETED or CANCELLED
        if (currentStatus == ShowtimeStatus.OPEN
                && requestedStatus != ShowtimeStatus.COMPLETED
                && requestedStatus != ShowtimeStatus.CANCELLED) {
            throw new BadRequestException(
                    "OPEN showtime can only transition to COMPLETED or CANCELLED");
        }
    }

    private boolean hasActiveBookings(Showtime showtime) {
        // Published showtimes may have bookings in another database. Do not bypass
        // Booking coordination by assuming that an unavailable service means no bookings.
        return showtime.getStatus() != ShowtimeStatus.SCHEDULED;
    }

    private LocalDateTime calculateEndTime(Movie movie, LocalDateTime startTime) {
        return startTime.plusMinutes(movie.getDurationMinutes()).plusMinutes(CLEANUP_MINUTES);
    }

    private void applyPricing(Showtime showtime, ShowtimeRequest request) {
        changePrices(showtime, request.basePrice(), request.vipPrice(), request.couplePrice());
        changeTicketPrices(
                showtime,
                request.adultStandardPrice(),
                request.childStandardPrice(),
                request.studentStandardPrice(),
                request.adultVipPrice(),
                request.childVipPrice(),
                request.studentVipPrice(),
                request.adultCouplePrice(),
                request.childCouplePrice(),
                request.studentCouplePrice(),
                Boolean.TRUE.equals(request.weekendSurcharge()),
                Boolean.TRUE.equals(request.holidaySurcharge()),
                request.lateNightSurchargeAmount()
        );
    }

    private void applyPricing(Showtime showtime, BulkShowtimeRequest request) {
        changePrices(showtime, request.basePrice(), request.vipPrice(), request.couplePrice());
        changeTicketPrices(
                showtime,
                request.adultStandardPrice(),
                request.childStandardPrice(),
                request.studentStandardPrice(),
                request.adultVipPrice(),
                request.childVipPrice(),
                request.studentVipPrice(),
                request.adultCouplePrice(),
                request.childCouplePrice(),
                request.studentCouplePrice(),
                Boolean.TRUE.equals(request.weekendSurcharge()),
                Boolean.TRUE.equals(request.holidaySurcharge()),
                request.lateNightSurchargeAmount()
        );
    }

    private void changePrices(Showtime showtime, java.math.BigDecimal basePrice,
                              java.math.BigDecimal vipPrice, java.math.BigDecimal couplePrice) {
        showtime.setBasePrice(basePrice);
        showtime.setVipPrice(vipPrice);
        showtime.setCouplePrice(couplePrice);
        showtime.setAdultStandardPrice(basePrice);
        showtime.setAdultVipPrice(vipPrice != null ? vipPrice : basePrice.add(java.math.BigDecimal.valueOf(20_000)));
        showtime.setAdultCouplePrice(couplePrice != null ? couplePrice : basePrice.add(java.math.BigDecimal.valueOf(30_000)));
    }

    private void changeTicketPrices(
            Showtime showtime,
            java.math.BigDecimal adultStandardPrice,
            java.math.BigDecimal childStandardPrice,
            java.math.BigDecimal studentStandardPrice,
            java.math.BigDecimal adultVipPrice,
            java.math.BigDecimal childVipPrice,
            java.math.BigDecimal studentVipPrice,
            java.math.BigDecimal adultCouplePrice,
            java.math.BigDecimal childCouplePrice,
            java.math.BigDecimal studentCouplePrice,
            boolean weekendSurcharge,
            boolean holidaySurcharge,
            java.math.BigDecimal lateNightSurchargeAmount
    ) {
        Room room = showtime.getRoom();
        Long cinemaId = room != null && room.getCinema() != null ? room.getCinema().getId() : null;

        java.math.BigDecimal baseStd = room != null && room.getStandardPrice() != null ? room.getStandardPrice() : defaultMoney(showtime.getBasePrice(), java.math.BigDecimal.valueOf(60_000));
        java.math.BigDecimal baseVip = room != null && room.getVipPrice() != null ? room.getVipPrice() : baseStd.add(java.math.BigDecimal.valueOf(20_000));
        java.math.BigDecimal baseCpl = room != null && room.getCouplePrice() != null ? room.getCouplePrice() : baseStd.add(java.math.BigDecimal.valueOf(30_000));

        java.math.BigDecimal surChild = java.math.BigDecimal.ZERO;
        java.math.BigDecimal surStudent = java.math.BigDecimal.ZERO;
        java.math.BigDecimal surAdult = java.math.BigDecimal.ZERO;

        if (cinemaId != null && audiencePriceRepository != null) {
            var audMap = audiencePriceRepository.findByCinemaId(cinemaId);
            for (var ap : audMap) {
                if (ap.getAudienceType() == AudienceType.CHILD && ap.getAdditionalPrice() != null) surChild = ap.getAdditionalPrice();
                else if (ap.getAudienceType() == AudienceType.STUDENT && ap.getAdditionalPrice() != null) surStudent = ap.getAdditionalPrice();
                else if (ap.getAudienceType() == AudienceType.ADULT && ap.getAdditionalPrice() != null) surAdult = ap.getAdditionalPrice();
            }
        }

        java.math.BigDecimal adultStandard = defaultMoney(adultStandardPrice, baseStd.add(surAdult));
        java.math.BigDecimal childStandard = defaultMoney(childStandardPrice, baseStd.add(surChild));
        java.math.BigDecimal studentStandard = defaultMoney(studentStandardPrice, baseStd.add(surStudent));
        java.math.BigDecimal adultVip = defaultMoney(adultVipPrice, baseVip.add(surAdult));
        java.math.BigDecimal childVip = defaultMoney(childVipPrice, baseVip.add(surChild));
        java.math.BigDecimal studentVip = defaultMoney(studentVipPrice, baseVip.add(surStudent));
        java.math.BigDecimal adultCouple = defaultMoney(adultCouplePrice, baseCpl.add(surAdult.multiply(java.math.BigDecimal.valueOf(2))));
        java.math.BigDecimal childCouple = defaultMoney(childCouplePrice, baseCpl.add(surChild.multiply(java.math.BigDecimal.valueOf(2))));
        java.math.BigDecimal studentCouple = defaultMoney(studentCouplePrice, baseCpl.add(surStudent.multiply(java.math.BigDecimal.valueOf(2))));

        showtime.setAdultStandardPrice(adultStandard);
        showtime.setChildStandardPrice(childStandard);
        showtime.setStudentStandardPrice(studentStandard);
        showtime.setAdultVipPrice(adultVip);
        showtime.setChildVipPrice(childVip);
        showtime.setStudentVipPrice(studentVip);
        showtime.setAdultCouplePrice(adultCouple);
        showtime.setChildCouplePrice(childCouple);
        showtime.setStudentCouplePrice(studentCouple);
        showtime.setWeekendSurcharge(weekendSurcharge);
        showtime.setHolidaySurcharge(holidaySurcharge);
        showtime.setLateNightSurchargeAmount(defaultMoney(lateNightSurchargeAmount, java.math.BigDecimal.valueOf(20_000)));
        showtime.setBasePrice(adultStandard);
        showtime.setVipPrice(adultVip);
        showtime.setCouplePrice(adultCouple);
    }

    private java.math.BigDecimal defaultMoney(java.math.BigDecimal value, java.math.BigDecimal fallback) {
        return value != null ? value : fallback;
    }

    private Movie findMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found"));
    }

    @Override
    @Transactional(readOnly = true)
    public Showtime findById(Long id) {
        return showtimeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
    }

    private void applyShowtimeCancellation(Showtime showtime, String reason) {
        showtime.setCancellationReason(reason == null || reason.isBlank() ? null : reason.trim());
        showtime.setCancelledAt(LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CustomerShowtimeSlotResponse> getCustomerAvailableSlots(Long movieId, LocalDate date) {
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(10);
        LocalDateTime from = date == null ? cutoff : date.atStartOfDay();
        if (from.isBefore(cutoff)) {
            from = cutoff;
        }
        LocalDateTime to = date == null ? LocalDate.now().plusYears(1).atStartOfDay() : date.plusDays(1).atStartOfDay();
        if (to.isBefore(from)) {
            return Collections.emptyList();
        }

        List<Showtime> candidates = showtimeRepository.findCustomerCandidateShowtimes(movieId, from, to);
        if (candidates.isEmpty()) {
            return Collections.emptyList();
        }

        Map<Long, Integer> availableSeatsMap = computeAvailableSeatsForShowtimes(candidates);

        List<ShowtimeAvailability> validShowtimes = candidates.stream()
                .map(st -> new ShowtimeAvailability(st, availableSeatsMap.getOrDefault(st.getId(), 0)))
                .filter(sa -> sa.availableSeats() > 0)
                .toList();

        if (validShowtimes.isEmpty()) {
            return Collections.emptyList();
        }

        Map<String, List<ShowtimeAvailability>> grouped = validShowtimes.stream()
                .collect(Collectors.groupingBy(
                        sa -> sa.showtime().getStartTime().toString() + "_" + resolveFormat(sa.showtime().getRoom().getRoomType()),
                        LinkedHashMap::new,
                        Collectors.toList()
                ));

        List<CustomerShowtimeSlotResponse> responses = new ArrayList<>();
        for (List<ShowtimeAvailability> slotList : grouped.values()) {
            ShowtimeAvailability best = slotList.stream()
                    .sorted(Comparator.comparingInt(ShowtimeAvailability::availableSeats).reversed()
                            .thenComparingLong(sa -> sa.showtime().getId()))
                    .findFirst()
                    .orElse(null);

            if (best == null) continue;

            java.math.BigDecimal minPrice = slotList.stream()
                    .map(sa -> sa.showtime().getBasePrice())
                    .filter(java.util.Objects::nonNull)
                    .min(java.math.BigDecimal::compareTo)
                    .orElse(best.showtime().getBasePrice());

            responses.add(new CustomerShowtimeSlotResponse(
                    best.showtime().getId(),
                    best.showtime().getMovie().getId(),
                    best.showtime().getMovie().getTitle(),
                    best.showtime().getStartTime(),
                    best.showtime().getEndTime(),
                    resolveFormat(best.showtime().getRoom().getRoomType()),
                    minPrice,
                    best.availableSeats(),
                    best.showtime().getRoom().getId(),
                    best.showtime().getBasePrice(),
                    best.showtime().getVipPrice(),
                    best.showtime().getCouplePrice(),
                    best.showtime().getAdultStandardPrice(),
                    best.showtime().getChildStandardPrice(),
                    best.showtime().getStudentStandardPrice(),
                    best.showtime().getAdultVipPrice(),
                    best.showtime().getChildVipPrice(),
                    best.showtime().getStudentVipPrice(),
                    best.showtime().getAdultCouplePrice(),
                    best.showtime().getChildCouplePrice(),
                    best.showtime().getStudentCouplePrice(),
                    best.showtime().getSurchargeAmount()
            ));
        }

        responses.sort(Comparator.comparing(CustomerShowtimeSlotResponse::startTime));
        return responses;
    }

    @Override
    @Transactional(readOnly = true)
    public CustomerShowtimeSlotResponse resolveCustomerShowtime(Long showtimeId) {
        Showtime showtime = findById(showtimeId);
        LocalDateTime cutoff = LocalDateTime.now().plusMinutes(10);

        boolean isSelfValid = (showtime.getStatus() == ShowtimeStatus.OPEN || showtime.getStatus() == ShowtimeStatus.SCHEDULED)
                && showtime.getRoom().getStatus() == RoomStatus.ACTIVE
                && showtime.getMovie().getStatus() != MovieStatus.INACTIVE
                && showtime.getStartTime().isAfter(cutoff);

        if (isSelfValid) {
            Map<Long, Integer> seatsMap = computeAvailableSeatsForShowtimes(List.of(showtime));
            int availableSeats = seatsMap.getOrDefault(showtime.getId(), 0);
            if (availableSeats > 0) {
                return new CustomerShowtimeSlotResponse(
                        showtime.getId(),
                        showtime.getMovie().getId(),
                        showtime.getMovie().getTitle(),
                        showtime.getStartTime(),
                        showtime.getEndTime(),
                        resolveFormat(showtime.getRoom().getRoomType()),
                        showtime.getBasePrice(),
                        availableSeats,
                        showtime.getRoom().getId(),
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
                        showtime.getSurchargeAmount()
                );
            }
        }

        if (!showtime.getStartTime().isAfter(cutoff)) {
            throw new BadRequestException("Khung giờ này vừa hết chỗ hoặc đã quá giờ đặt vé. Vui lòng chọn khung giờ khác.");
        }

        List<Showtime> candidates = showtimeRepository.findEquivalentCandidateShowtimes(
                showtime.getMovie().getId(),
                showtime.getStartTime()
        );

        if (!candidates.isEmpty()) {
            Map<Long, Integer> seatsMap = computeAvailableSeatsForShowtimes(candidates);
            ShowtimeAvailability best = candidates.stream()
                    .map(st -> new ShowtimeAvailability(st, seatsMap.getOrDefault(st.getId(), 0)))
                    .filter(sa -> sa.availableSeats() > 0)
                    .sorted(Comparator.comparingInt(ShowtimeAvailability::availableSeats).reversed()
                            .thenComparingLong(sa -> sa.showtime().getId()))
                    .findFirst()
                    .orElse(null);

            if (best != null) {
                return new CustomerShowtimeSlotResponse(
                        best.showtime().getId(),
                        best.showtime().getMovie().getId(),
                        best.showtime().getMovie().getTitle(),
                        best.showtime().getStartTime(),
                        best.showtime().getEndTime(),
                        resolveFormat(best.showtime().getRoom().getRoomType()),
                        best.showtime().getBasePrice(),
                        best.availableSeats(),
                        best.showtime().getRoom().getId(),
                        best.showtime().getBasePrice(),
                        best.showtime().getVipPrice(),
                        best.showtime().getCouplePrice(),
                        best.showtime().getAdultStandardPrice(),
                        best.showtime().getChildStandardPrice(),
                        best.showtime().getStudentStandardPrice(),
                        best.showtime().getAdultVipPrice(),
                        best.showtime().getChildVipPrice(),
                        best.showtime().getStudentVipPrice(),
                        best.showtime().getAdultCouplePrice(),
                        best.showtime().getChildCouplePrice(),
                        best.showtime().getStudentCouplePrice(),
                        best.showtime().getSurchargeAmount()
                );
            }
        }

        throw new BadRequestException("Khung giờ này vừa hết chỗ. Vui lòng chọn khung giờ khác.");
    }

    private record ShowtimeAvailability(Showtime showtime, int availableSeats) {}

    private String resolveFormat(RoomType roomType) {
        if (roomType == null) return "2D";
        return switch (roomType) {
            case THREE_D -> "3D";
            case IMAX -> "IMAX";
            case VIP -> "VIP";
            case TWO_D, STANDARD -> "2D";
            default -> "2D";
        };
    }

    private Map<Long, Integer> computeAvailableSeatsForShowtimes(List<Showtime> showtimes) {
        if (showtimes == null || showtimes.isEmpty()) {
            return Collections.emptyMap();
        }
        Map<Long, Integer> result = new HashMap<>();
        for (Showtime st : showtimes) {
            int totalSeats = seatRepository.findByRoom(st.getRoom()).size();
            int occupiedSeats = 0;
            try {
                List<com.cinemaai.catalog.client.BookingClient.OccupiedSeat> occupied =
                        bookingClient.getOccupiedSeats(st.getId());
                if (occupied != null) {
                    occupiedSeats = occupied.size();
                }
            } catch (Exception ex) {
                log.warn("Failed to fetch occupied seats for showtime {}: {}", st.getId(), ex.getMessage());
            }
            result.put(st.getId(), Math.max(0, totalSeats - occupiedSeats));
        }
        return result;
    }

    // =========================================================================
    // PRICE PREVIEW (NO PERSISTENCE)
    // =========================================================================

    /**
     * Computes the full ticket price matrix for a set of draft showtime slots.
     * Does NOT persist any data; safe to call multiple times without side effects.
     *
     * <p>Formula:
     * <pre>
     *   standardPrice(audience) = room.standardPrice + audience.additionalPrice
     *   vipPrice(audience)      = room.vipPrice      + audience.additionalPrice
     *   couplePrice(a1, a2)     = room.couplePrice   + a1.additionalPrice + a2.additionalPrice
     * </pre>
     */
    @Transactional(readOnly = true)
    public List<ShowtimePricePreviewResponse> previewPrices(ShowtimePreviewRequest request) {
        Movie movie = movieRepository.findById(request.movieId())
                .orElseThrow(() -> new NotFoundException("Movie not found: " + request.movieId()));

        List<ShowtimePricePreviewResponse> results = new ArrayList<>();

        for (ShowtimePreviewRequest.PreviewSlot slot : request.slots()) {
            Room room = roomService.findById(slot.roomId());
            Long cinemaId = room.getCinema().getId();

            // Load audience surcharges for this cinema
            Map<AudienceType, java.math.BigDecimal> surchargeMap = audiencePriceRepository
                    .findByCinemaId(cinemaId)
                    .stream()
                    .collect(Collectors.toMap(
                            CinemaAudiencePrice::getAudienceType,
                            CinemaAudiencePrice::getAdditionalPrice
                    ));

            boolean audiencePriceMissing = surchargeMap.size() < 3
                    || !surchargeMap.containsKey(AudienceType.CHILD)
                    || !surchargeMap.containsKey(AudienceType.STUDENT)
                    || !surchargeMap.containsKey(AudienceType.ADULT);

            java.math.BigDecimal childAdd   = surchargeMap.getOrDefault(AudienceType.CHILD,   java.math.BigDecimal.ZERO);
            java.math.BigDecimal studentAdd = surchargeMap.getOrDefault(AudienceType.STUDENT, java.math.BigDecimal.ZERO);
            java.math.BigDecimal adultAdd   = surchargeMap.getOrDefault(AudienceType.ADULT,   java.math.BigDecimal.ZERO);

            // Room base prices (fall back to 0 if null)
            java.math.BigDecimal roomStd    = room.getStandardPrice() != null ? room.getStandardPrice() : java.math.BigDecimal.ZERO;
            java.math.BigDecimal roomVip    = room.getVipPrice()      != null ? room.getVipPrice()      : java.math.BigDecimal.ZERO;
            java.math.BigDecimal roomCouple = room.getCouplePrice()   != null ? room.getCouplePrice()   : java.math.BigDecimal.ZERO;

            // Standard seat prices
            java.math.BigDecimal childStd   = roomStd.add(childAdd);
            java.math.BigDecimal studentStd = roomStd.add(studentAdd);
            java.math.BigDecimal adultStd   = roomStd.add(adultAdd);

            // VIP seat prices
            java.math.BigDecimal childVip   = roomVip.add(childAdd);
            java.math.BigDecimal studentVip = roomVip.add(studentAdd);
            java.math.BigDecimal adultVip   = roomVip.add(adultAdd);

            // Couple seat prices (all combinations: price per seat-pair = room base + surcharge1 + surcharge2)
            java.math.BigDecimal ccCouple  = roomCouple.add(childAdd).add(childAdd);
            java.math.BigDecimal csCouple  = roomCouple.add(childAdd).add(studentAdd);
            java.math.BigDecimal caCouple  = roomCouple.add(childAdd).add(adultAdd);
            java.math.BigDecimal ssCouple  = roomCouple.add(studentAdd).add(studentAdd);
            java.math.BigDecimal saCouple  = roomCouple.add(studentAdd).add(adultAdd);
            java.math.BigDecimal aaCouple  = roomCouple.add(adultAdd).add(adultAdd);

            // Calculate end time
            LocalDateTime endTime = calculateEndTime(movie, slot.startTime());

            List<String> warnings = new ArrayList<>();
            if (audiencePriceMissing) {
                warnings.add("Rạp chưa cấu hình đủ giá vé theo đối tượng (CHILD/STUDENT/ADULT). Cần thiết lập trước khi lưu.");
            }
            if (slot.startTime().isBefore(LocalDateTime.now())) {
                warnings.add("Thời gian bắt đầu đã qua hiện tại.");
            }

            results.add(new ShowtimePricePreviewResponse(
                    slot.tempId(),
                    movie.getId(),
                    movie.getTitle(),
                    room.getId(),
                    room.getName(),
                    cinemaId,
                    room.getCinema().getName(),
                    slot.startTime(),
                    endTime,
                    roomStd,
                    roomVip,
                    roomCouple,
                    childAdd,
                    studentAdd,
                    adultAdd,
                    childStd,
                    studentStd,
                    adultStd,
                    childVip,
                    studentVip,
                    adultVip,
                    ccCouple,
                    csCouple,
                    caCouple,
                    ssCouple,
                    saCouple,
                    aaCouple,
                    audiencePriceMissing,
                    warnings
            ));
        }
        return results;
    }
}

