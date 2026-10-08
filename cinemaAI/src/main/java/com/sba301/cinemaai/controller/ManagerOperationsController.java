package com.sba301.cinemaai.controller;

import com.sba301.cinemaai.dto.request.food.StockAdjustmentRequest;
import com.sba301.cinemaai.dto.request.cinema.ShowtimeRequest;
import com.sba301.cinemaai.dto.request.cinema.SeatOperationalStatusRequest;
import com.sba301.cinemaai.dto.request.user.ManagerStaffCreateRequest;
import com.sba301.cinemaai.dto.request.user.AdminUserStatusUpdateRequest;
import com.sba301.cinemaai.dto.response.ApiResponse;
import com.sba301.cinemaai.dto.response.PageResponse;
import com.sba301.cinemaai.dto.response.audit.AuditLogResponse;
import com.sba301.cinemaai.dto.response.booking.BookingResponse;
import com.sba301.cinemaai.dto.response.cinema.CinemaResponse;
import com.sba301.cinemaai.dto.response.cinema.RoomResponse;
import com.sba301.cinemaai.dto.response.cinema.SeatResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeResponse;
import com.sba301.cinemaai.dto.response.cinema.ShowtimeSeatMapResponse;
import com.sba301.cinemaai.dto.response.cinema.AvailableSlotResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventoryTransactionResponse;
import com.sba301.cinemaai.dto.response.food.FoodInventorySummaryResponse;
import com.sba301.cinemaai.dto.response.report.CinemaOverviewReportResponse;
import com.sba301.cinemaai.dto.response.user.UserProfileResponse;
import com.sba301.cinemaai.entity.Booking;
import com.sba301.cinemaai.entity.Cinema;
import com.sba301.cinemaai.entity.Role;
import com.sba301.cinemaai.entity.StaffCinemaAssignment;
import com.sba301.cinemaai.entity.User;
import com.sba301.cinemaai.entity.UserRole;
import com.sba301.cinemaai.enums.AuditActionType;
import com.sba301.cinemaai.enums.FoodStockStatus;
import com.sba301.cinemaai.enums.RoleName;
import com.sba301.cinemaai.enums.ShowtimeStatus;
import com.sba301.cinemaai.enums.UserStatus;
import com.sba301.cinemaai.exception.ConflictException;
import com.sba301.cinemaai.exception.ForbiddenException;
import com.sba301.cinemaai.exception.NotFoundException;
import com.sba301.cinemaai.mapper.UserMapper;
import com.sba301.cinemaai.repository.BookingRepository;
import com.sba301.cinemaai.repository.CinemaRepository;
import com.sba301.cinemaai.repository.RoleRepository;
import com.sba301.cinemaai.repository.StaffCinemaAssignmentRepository;
import com.sba301.cinemaai.repository.UserRepository;
import com.sba301.cinemaai.repository.UserRoleRepository;
import com.sba301.cinemaai.security.AuthenticatedUser;
import com.sba301.cinemaai.service.AuditLogService;
import com.sba301.cinemaai.service.BookingService;
import com.sba301.cinemaai.service.CinemaService;
import com.sba301.cinemaai.service.FoodInventoryService;
import com.sba301.cinemaai.service.ManagerAccountService;
import com.sba301.cinemaai.service.RoomService;
import com.sba301.cinemaai.service.ShowtimeService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/manager")
@RequiredArgsConstructor
@SecurityRequirement(name = "Bearer Authentication")
public class ManagerOperationsController {
    private final ManagerAccountService managerAccountService;
    private final CinemaService cinemaService;
    private final RoomService roomService;
    private final ShowtimeService showtimeService;
    private final FoodInventoryService foodInventoryService;
    private final BookingRepository bookingRepository;
    private final BookingService bookingService;
    private final StaffCinemaAssignmentRepository staffCinemaAssignmentRepository;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final UserRoleRepository userRoleRepository;
    private final CinemaRepository cinemaRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;
    private final AuditLogService auditLogService;

    // ── Cinemas ─────────────────────────────────────────────────────────────

    @GetMapping("/cinemas")
    public ApiResponse<List<CinemaResponse>> myCinemas(@AuthenticationPrincipal AuthenticatedUser user) {
        return ApiResponse.success(managerAccountService.assignedCinemaIds(user.id()).stream()
                .map(cinemaService::getCinema).toList());
    }

    // ── Rooms & Seats ───────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/rooms")
    public ApiResponse<List<RoomResponse>> rooms(@PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(roomService.getRoomsByCinema(cinemaId));
    }

    @GetMapping("/cinemas/{cinemaId}/rooms/{roomId}/seats")
    public ApiResponse<List<SeatResponse>> seats(@PathVariable Long cinemaId, @PathVariable Long roomId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        requireRoomCinema(cinemaId, roomId);
        return ApiResponse.success(roomService.getSeats(roomId));
    }

    @PutMapping("/cinemas/{cinemaId}/seats/{seatId}/status")
    public ApiResponse<SeatResponse> updateSeatOperationalStatus(@PathVariable Long cinemaId, @PathVariable Long seatId,
            @Valid @RequestBody SeatOperationalStatusRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        if (!cinemaId.equals(roomService.getSeatCinemaId(seatId))) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        }
        SeatResponse res = roomService.updateSeatOperationalStatus(seatId, request);
        auditLogService.record(AuditActionType.UPDATE, "SEAT", seatId,
                "Manager changed seat operational status to " + request.status() + " reason=" + request.reason(), cinemaId);
        return ApiResponse.success(res);
    }

    // ── Showtimes ───────────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/showtimes")
    public ApiResponse<PageResponse<ShowtimeResponse>> showtimes(@PathVariable Long cinemaId,
            @RequestParam(required = false) Long movieId,
            @RequestParam(required = false) Long roomId,
            @RequestParam(required = false) ShowtimeStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(showtimeService.searchAdmin(movieId, roomId, cinemaId, status, date, page, size));
    }

    @GetMapping("/cinemas/{cinemaId}/showtimes/{showtimeId}")
    public ApiResponse<ShowtimeResponse> showtime(@PathVariable Long cinemaId, @PathVariable Long showtimeId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        ShowtimeResponse response = showtimeService.getAdmin(showtimeId);
        if (!cinemaId.equals(response.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        return ApiResponse.success(response);
    }

    @GetMapping("/cinemas/{cinemaId}/showtimes/{showtimeId}/seat-map")
    public ApiResponse<ShowtimeSeatMapResponse> seatMap(@PathVariable Long cinemaId, @PathVariable Long showtimeId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        ShowtimeResponse response = showtimeService.getAdmin(showtimeId);
        if (!cinemaId.equals(response.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        return ApiResponse.success(showtimeService.getSeatMap(showtimeId));
    }

    @GetMapping("/cinemas/{cinemaId}/showtimes/available-slots")
    public ApiResponse<List<AvailableSlotResponse>> availableSlots(@PathVariable Long cinemaId,
            @RequestParam Long roomId, @RequestParam Long movieId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        requireRoomCinema(cinemaId, roomId);
        return ApiResponse.success(showtimeService.getAvailableSlots(roomId, movieId, date));
    }

    @PostMapping("/cinemas/{cinemaId}/showtimes")
    public ApiResponse<ShowtimeResponse> createShowtime(@PathVariable Long cinemaId,
            @Valid @RequestBody ShowtimeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        requireRoomCinema(cinemaId, request.roomId());
        ShowtimeResponse res = showtimeService.create(request);
        auditLogService.record(AuditActionType.CREATE, "SHOWTIME", res.id(),
                "Manager created showtime for movie #" + request.movieId() + " in room #" + request.roomId(), cinemaId);
        return ApiResponse.success(res, "Showtime created successfully");
    }

    @PutMapping("/cinemas/{cinemaId}/showtimes/{showtimeId}")
    public ApiResponse<ShowtimeResponse> updateShowtime(@PathVariable Long cinemaId, @PathVariable Long showtimeId,
            @Valid @RequestBody ShowtimeRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        ShowtimeResponse current = showtimeService.getAdmin(showtimeId);
        if (!cinemaId.equals(current.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        requireRoomCinema(cinemaId, request.roomId());
        ShowtimeResponse res = showtimeService.update(showtimeId, request);
        auditLogService.record(AuditActionType.UPDATE, "SHOWTIME", showtimeId,
                "Manager updated showtime #" + showtimeId, cinemaId);
        return ApiResponse.success(res, "Showtime updated successfully");
    }

    @PostMapping("/cinemas/{cinemaId}/showtimes/{showtimeId}/cancel")
    public ApiResponse<ShowtimeResponse> cancelShowtime(@PathVariable Long cinemaId, @PathVariable Long showtimeId,
            @RequestParam String reason, @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        ShowtimeResponse current = showtimeService.getAdmin(showtimeId);
        if (!cinemaId.equals(current.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        ShowtimeResponse res = showtimeService.cancelShowtime(showtimeId, reason);
        auditLogService.record(AuditActionType.DELETE, "SHOWTIME", showtimeId,
                "Manager cancelled showtime #" + showtimeId + " reason=" + reason, cinemaId);
        return ApiResponse.success(res, "Showtime cancelled and refund process initiated");
    }

    // ── F&B Inventory ───────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/inventory")
    public ApiResponse<List<FoodInventoryResponse>> inventory(@PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(foodInventoryService.getInventoriesByCinema(cinemaId));
    }

    @GetMapping("/cinemas/{cinemaId}/inventory/low-stock")
    public ApiResponse<List<FoodInventoryResponse>> lowStock(@PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(foodInventoryService.getInventoriesByCinema(cinemaId).stream()
                .filter(item -> item.stockStatus() == FoodStockStatus.LOW_STOCK
                        || item.stockStatus() == FoodStockStatus.OUT_OF_STOCK)
                .toList());
    }

    @GetMapping("/cinemas/{cinemaId}/inventory/summary")
    public ApiResponse<FoodInventorySummaryResponse> inventorySummary(@PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        List<FoodInventoryResponse> inventory = foodInventoryService.getInventoriesByCinema(cinemaId);
        return ApiResponse.success(new FoodInventorySummaryResponse(
                cinemaId,
                inventory.size(),
                inventory.stream().mapToInt(FoodInventoryResponse::availableQuantity).sum(),
                (int) inventory.stream().filter(item -> item.stockStatus() == FoodStockStatus.LOW_STOCK).count(),
                (int) inventory.stream().filter(item -> item.stockStatus() == FoodStockStatus.OUT_OF_STOCK).count()));
    }

    @GetMapping("/cinemas/{cinemaId}/inventory/transactions")
    public ApiResponse<PageResponse<FoodInventoryTransactionResponse>> inventoryTransactions(
            @PathVariable Long cinemaId, @RequestParam(required = false) Long foodItemId,
            @RequestParam(defaultValue = "0") int page, @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(foodInventoryService.getTransactions(cinemaId, foodItemId, page, size));
    }

    @PostMapping("/cinemas/{cinemaId}/inventory/adjust")
    public ApiResponse<FoodInventoryResponse> adjustInventory(@PathVariable Long cinemaId,
            @Valid @RequestBody StockAdjustmentRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        if (!cinemaId.equals(request.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        FoodInventoryResponse res = foodInventoryService.adjustStock(request, user.email());
        auditLogService.record(AuditActionType.UPDATE, "FOOD_INVENTORY", request.foodItemId(),
                "Manager adjusted stock type=" + request.type() + " delta=" + request.quantityDelta() + " reason=" + request.reason(), cinemaId);
        return ApiResponse.success(res);
    }

    // ── Reports & Overview ──────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/reports/overview")
    public ApiResponse<CinemaOverviewReportResponse> cinemaOverviewReport(
            @PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new NotFoundException("Cinema not found"));
        LocalDate today = LocalDate.now();
        LocalDateTime todayStart = today.atStartOfDay();
        LocalDateTime todayEnd = today.plusDays(1).atStartOfDay();
        LocalDateTime allTimeStart = LocalDateTime.of(2000, 1, 1, 0, 0);

        BigDecimal todayRev = bookingRepository.sumRevenueByCinema(cinemaId, todayStart, todayEnd);
        BigDecimal totalRev = bookingRepository.sumRevenueByCinema(cinemaId, allTimeStart, todayEnd);
        long todayTickets = bookingRepository.countTicketsSoldByCinema(cinemaId, todayStart, todayEnd);
        long totalTickets = bookingRepository.countTicketsSoldByCinema(cinemaId, allTimeStart, todayEnd);
        long todayBookings = bookingRepository.countBookingsByCinema(cinemaId, todayStart, todayEnd);
        long totalBookings = bookingRepository.countBookingsByCinema(cinemaId, allTimeStart, todayEnd);

        List<RoomResponse> rooms = roomService.getRoomsByCinema(cinemaId);
        int totalCapacity = rooms.stream().mapToInt(r -> r.rowCount() * r.columnCount()).sum();
        List<ShowtimeResponse> todayShowtimes = showtimeService.searchAdmin(null, null, cinemaId, null, today, 0, 100).items();
        long maxPossibleSeats = (long) totalCapacity * Math.max(todayShowtimes.size(), 1);
        double occupancyRate = maxPossibleSeats > 0 ? (double) todayTickets / maxPossibleSeats * 100.0 : 0.0;

        List<FoodInventoryResponse> inventory = foodInventoryService.getInventoriesByCinema(cinemaId);
        int lowStock = (int) inventory.stream().filter(i -> i.stockStatus() == FoodStockStatus.LOW_STOCK).count();
        int outOfStock = (int) inventory.stream().filter(i -> i.stockStatus() == FoodStockStatus.OUT_OF_STOCK).count();

        return ApiResponse.success(new CinemaOverviewReportResponse(
                cinemaId,
                cinema.getName(),
                todayRev,
                totalRev,
                todayTickets,
                totalTickets,
                todayBookings,
                totalBookings,
                Math.round(occupancyRate * 10.0) / 10.0,
                rooms.size(),
                lowStock,
                outOfStock,
                todayShowtimes.size()
        ));
    }

    // ── Bookings ────────────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/bookings")
    public ApiResponse<PageResponse<BookingResponse>> cinemaBookings(
            @PathVariable Long cinemaId,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        Pageable pageable = PageRequest.of(Math.max(page, 0), Math.max(1, Math.min(size, 100)));
        Page<Booking> bookings = bookingRepository.searchByCinema(cinemaId, keyword != null ? keyword.trim() : null, pageable);
        return ApiResponse.success(PageResponse.from(bookings.map(b -> bookingService.getAdminBooking(b.getId()))));
    }

    @GetMapping("/cinemas/{cinemaId}/bookings/{bookingId}")
    public ApiResponse<BookingResponse> cinemaBookingDetail(
            @PathVariable Long cinemaId,
            @PathVariable Long bookingId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new NotFoundException("Booking not found"));
        if (!cinemaId.equals(booking.getShowtime().getRoom().getCinema().getId())) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        }
        return ApiResponse.success(bookingService.getAdminBooking(bookingId));
    }

    // ── Staff Management ────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/staff")
    public ApiResponse<List<UserProfileResponse>> cinemaStaff(
            @PathVariable Long cinemaId,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        List<StaffCinemaAssignment> assignments = staffCinemaAssignmentRepository.findByCinemaId(cinemaId);
        List<UserProfileResponse> staffList = assignments.stream()
                .map(a -> {
                    User u = a.getUser();
                    List<String> roles = userRoleRepository.findByUserId(u.getId())
                            .stream().map(ur -> ur.getRole().getName().name()).toList();
                    return userMapper.toProfile(u, roles);
                })
                .toList();
        return ApiResponse.success(staffList);
    }

    @PostMapping("/cinemas/{cinemaId}/staff")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<UserProfileResponse> createCinemaStaff(
            @PathVariable Long cinemaId,
            @Valid @RequestBody ManagerStaffCreateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        if (userRepository.existsByEmail(request.email())) {
            throw new ConflictException("Email already exists");
        }
        Cinema cinema = cinemaRepository.findById(cinemaId)
                .orElseThrow(() -> new NotFoundException("Cinema not found"));

        User staff = new User(request.email(), passwordEncoder.encode(request.password()),
                request.fullName(), request.phone(), request.birthYear());
        staff.setStatus(UserStatus.ACTIVE);
        staff.setEmailVerified(true);
        userRepository.save(staff);

        Role staffRole = roleRepository.findByName(RoleName.STAFF)
                .orElseGet(() -> roleRepository.save(new Role(RoleName.STAFF)));
        userRoleRepository.save(new UserRole(staff, staffRole));
        staffCinemaAssignmentRepository.save(new StaffCinemaAssignment(staff, cinema));

        auditLogService.record(AuditActionType.CREATE, "STAFF", staff.getId(),
                "Manager created staff " + staff.getEmail() + " for cinema #" + cinemaId, cinemaId);

        return ApiResponse.success(userMapper.toProfile(staff, List.of("STAFF")), "Staff account created");
    }

    @PatchMapping("/cinemas/{cinemaId}/staff/{staffId}/status")
    public ApiResponse<UserProfileResponse> updateStaffStatus(
            @PathVariable Long cinemaId,
            @PathVariable Long staffId,
            @Valid @RequestBody AdminUserStatusUpdateRequest request,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        if (!staffCinemaAssignmentRepository.existsByUserIdAndCinemaId(staffId, cinemaId)) {
            throw new ForbiddenException("CINEMA_ACCESS_DENIED");
        }
        User staff = userRepository.findById(staffId)
                .orElseThrow(() -> new NotFoundException("User not found"));

        boolean isStaffOnly = userRoleRepository.findByUserId(staffId).stream()
                .allMatch(ur -> ur.getRole().getName() == RoleName.STAFF || ur.getRole().getName() == RoleName.CUSTOMER);
        if (!isStaffOnly) {
            throw new ForbiddenException("FORBIDDEN_ROLE_CHANGE");
        }

        staff.setStatus(request.status());
        userRepository.save(staff);

        auditLogService.record(AuditActionType.UPDATE, "STAFF", staff.getId(),
                "Manager updated staff status to " + request.status(), cinemaId);

        List<String> roles = userRoleRepository.findByUserId(staff.getId())
                .stream().map(ur -> ur.getRole().getName().name()).toList();
        return ApiResponse.success(userMapper.toProfile(staff, roles), "Staff status updated");
    }

    // ── Audit Logs ──────────────────────────────────────────────────────────

    @GetMapping("/cinemas/{cinemaId}/audit-logs")
    public ApiResponse<PageResponse<AuditLogResponse>> cinemaAuditLogs(
            @PathVariable Long cinemaId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal AuthenticatedUser user
    ) {
        managerAccountService.requireCinemaAccess(user.id(), cinemaId);
        return ApiResponse.success(auditLogService.getLogsByCinema(cinemaId, page, size));
    }

    // ── Helpers ─────────────────────────────────────────────────────────────

    private void requireRoomCinema(Long cinemaId, Long roomId) {
        RoomResponse room = roomService.getRoom(roomId);
        if (!cinemaId.equals(room.cinemaId())) throw new ForbiddenException("CINEMA_ACCESS_DENIED");
    }
}
