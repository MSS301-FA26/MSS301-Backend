package com.sba301.cinemaai.service.impl;

import com.sba301.cinemaai.dto.request.cinema.BulkShowtimeRequest;
import com.sba301.cinemaai.dto.request.cinema.ShowtimeRequest;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatMapResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatResponse;
import com.sba301.cinemaai.dto.response.refund.BulkRefundResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.BookingSeat;
import com.sba301.cinemaai.entity.Movie;
import com.sba301.cinemaai.entity.Room;
import com.sba301.cinemaai.entity.Seat;
import com.sba301.cinemaai.entity.Showtime;
import com.sba301.cinemaai.enums.*;
import com.sba301.cinemaai.exception.BadRequestException;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.CinemaMapper;
import com.sba301.cinemaai.repository.*;
import com.sba301.cinemaai.dto.response.cinema.AvailableSlotResponse;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.RefundService;
import com.sba301.cinemaai.service.RoomService;
import com.sba301.cinemaai.service.ShowtimeService;
import com.sba301.cinemaai.service.BulkRefundService;
import java.time.LocalTime;
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
    private static final List<BookingStatus> ACTIVE_BOOKING_STATUSES = List.of(
            BookingStatus.HOLDING,
            BookingStatus.PENDING_PAYMENT,
            BookingStatus.PAID,
            BookingStatus.REFUND_REQUESTED
    );

    private final ShowtimeRepository showtimeRepository;
    private final MovieRepository movieRepository;
    private final SeatRepository seatRepository;
    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final MovieGenreRepository movieGenreRepository;
    private final RoomService roomService;
    private final CinemaMapper cinemaMapper;
    private final RefundService refundService;
    private final BulkRefundService bulkRefundService;
    private final AuditLogService auditLogService;


    // -------------------------------------------------------------------------
    // PUBLIC (customer-facing)
    // -------------------------------------------------------------------------

    /**
     * Public showtime search: only returns OPEN showtimes.
     * SCHEDULED is an internal admin state — customers cannot book it.
     */
    @Transactional(readOnly = true)
    public PageResponse<ShowtimeResponse> searchPublic(Long movieId, Long roomId, LocalDate date, int page, int size) {
        LocalDateTime from = date == null ? LocalDate.now().atStartOfDay() : date.atStartOfDay();
        LocalDateTime to = date == null ? LocalDate.now().plusYears(1).atStartOfDay() : date.plusDays(1).atStartOfDay();
        LocalDateTime now = LocalDateTime.now();
        if (from.isBefore(now)) {
            from = now;
        }

        org.springframework.data.domain.Page<Showtime> showtimePage =
                showtimeRepository.searchPublic(movieId, roomId, from, to,
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
        return cinemaMapper.toShowtimeResponse(findById(id));
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
            refundService.processShowtimeCancellation(showtime, null);
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
        refundService.processShowtimeCancellation(showtime, reason);
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        auditLogService.record(AuditActionType.DELETE, "SHOWTIME", showtime.getId(),
                "Cancelled: " + (reason == null ? "" : reason));
        return cinemaMapper.toShowtimeResponse(showtime);
    }

    @Transactional
    @Override
    public BulkRefundResponse cancelShowtimeAndRefund(Long id, String reason) {
        Showtime showtime = showtimeRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
        validateStatusTransition(showtime, ShowtimeStatus.CANCELLED);
        applyShowtimeCancellation(showtime, reason);
        showtime.setStatus(ShowtimeStatus.CANCELLED);
        auditLogService.record(AuditActionType.REFUND, "SHOWTIME", showtime.getId(),
                "Cancelled with bulk refund: " + (reason == null ? "" : reason));
        return bulkRefundService.processBulkRefund(showtime, reason);
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
        Map<Long, BookingSeat> runtimeSeats = bookingSeatRepository.findByShowtime(showtime)
                .stream()
                .filter(this::isSeatMapBlockingSeat)
                .collect(Collectors.toMap(
                        bookingSeat -> bookingSeat.getSeat().getId(),
                        Function.identity(),
                        (left, right) -> left
                ));

        List<ShowtimeSeatResponse> seatResponses = seats.stream()
                .map(seat -> {
                    BookingSeat bookingSeat = runtimeSeats.get(seat.getId());
                    return cinemaMapper.toShowtimeSeatResponse(
                            seat,
                            resolveRuntimeStatus(seat, runtimeSeats),
                            resolveHoldExpiresAt(bookingSeat),
                            showtime
                    );
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
        return bookingRepository.existsByShowtimeAndStatusIn(showtime, ACTIVE_BOOKING_STATUSES);
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
        java.math.BigDecimal adultStandard = defaultMoney(adultStandardPrice, showtime.getBasePrice());
        java.math.BigDecimal childStandard = defaultMoney(childStandardPrice, adultStandard);
        java.math.BigDecimal studentStandard = defaultMoney(studentStandardPrice, adultStandard);
        java.math.BigDecimal adultVip = defaultMoney(adultVipPrice, adultStandard.add(java.math.BigDecimal.valueOf(20_000)));
        java.math.BigDecimal childVip = defaultMoney(childVipPrice, childStandard.add(java.math.BigDecimal.valueOf(20_000)));
        java.math.BigDecimal studentVip = defaultMoney(studentVipPrice, studentStandard.add(java.math.BigDecimal.valueOf(20_000)));
        java.math.BigDecimal adultCouple = defaultMoney(adultCouplePrice, adultStandard.add(java.math.BigDecimal.valueOf(30_000)));
        java.math.BigDecimal childCouple = defaultMoney(childCouplePrice, childStandard.add(java.math.BigDecimal.valueOf(30_000)));
        java.math.BigDecimal studentCouple = defaultMoney(studentCouplePrice, studentStandard.add(java.math.BigDecimal.valueOf(30_000)));

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

    private String resolveRuntimeStatus(Seat seat, Map<Long, BookingSeat> runtimeSeats) {
        if (seat.getStatus() != SeatStatus.AVAILABLE) {
            return "UNAVAILABLE";
        }
        BookingSeat bookingSeat = runtimeSeats.get(seat.getId());
        if (bookingSeat == null) {
            return "AVAILABLE";
        }
        return bookingSeat.getStatus().name();
    }

    private LocalDateTime resolveHoldExpiresAt(BookingSeat bookingSeat) {
        if (bookingSeat == null) {
            return null;
        }
        Booking booking = bookingSeat.getBooking();
        boolean holdLikeBooking = booking.getStatus() == BookingStatus.HOLDING
                || booking.getStatus() == BookingStatus.PENDING_PAYMENT;
        boolean holdLikeSeat = bookingSeat.getStatus() == SeatRuntimeStatus.HOLDING
                || bookingSeat.getStatus() == SeatRuntimeStatus.BOOKED;
        if (!holdLikeBooking || !holdLikeSeat || booking.getHoldExpiresAt() == null) {
            return null;
        }
        return booking.getHoldExpiresAt().isAfter(LocalDateTime.now()) ? booking.getHoldExpiresAt() : null;
    }

    private boolean isSeatMapBlockingSeat(BookingSeat bookingSeat) {
        if (bookingSeat.getStatus() == SeatRuntimeStatus.RELEASED) {
            return false;
        }
        Booking booking = bookingSeat.getBooking();
        if (booking.getStatus() == BookingStatus.HOLDING || booking.getStatus() == BookingStatus.PENDING_PAYMENT) {
            return booking.getHoldExpiresAt() == null || booking.getHoldExpiresAt().isAfter(LocalDateTime.now());
        }
        return bookingSeat.getStatus() == SeatRuntimeStatus.BOOKED
                || bookingSeat.getStatus() == SeatRuntimeStatus.CHECKED_IN;
    }

    private Movie findMovie(Long movieId) {
        return movieRepository.findById(movieId)
                .orElseThrow(() -> new NotFoundException("Movie not found"));
    }

    private Showtime findById(Long id) {
        return showtimeRepository.findWithDetailsById(id)
                .orElseThrow(() -> new NotFoundException("Showtime not found"));
    }

    private void applyShowtimeCancellation(Showtime showtime, String reason) {
        showtime.setCancellationReason(reason == null || reason.isBlank() ? null : reason.trim());
        showtime.setCancelledAt(LocalDateTime.now());
    }

}
