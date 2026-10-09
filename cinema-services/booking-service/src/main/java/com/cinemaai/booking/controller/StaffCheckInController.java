package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.FoodOrder;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import com.cinemaai.booking.repository.BookingSeatRepository;
import com.cinemaai.booking.repository.FoodOrderRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/staff/check-in")
@RequiredArgsConstructor
@Tag(name = "Staff Check-In", description = "Quét mã QR và soát vé vào rạp")
public class StaffCheckInController {

    private final BookingRepository bookingRepository;
    private final BookingSeatRepository bookingSeatRepository;
    private final FoodOrderRepository foodOrderRepository;
    private final com.cinemaai.booking.security.CinemaSecurityService cinemaSecurityService;

    @Operation(summary = "Tra cứu thông tin vé để soát vé qua GET")
    @GetMapping("/lookup")
    @Transactional
    public ApiResponse<BookingResponse> lookupGet(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String qrCode
    ) {
        return processLookup(user, bookingCode, code, qrCode);
    }

    @Operation(summary = "Tra cứu thông tin vé để soát vé qua POST")
    @PostMapping("/lookup")
    @Transactional
    public ApiResponse<BookingResponse> lookupPost(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String qrCode,
            @RequestBody(required = false) Map<String, String> body
    ) {
        String queryCode = bookingCode;
        if (queryCode == null || queryCode.isBlank()) queryCode = code;
        if (queryCode == null || queryCode.isBlank()) queryCode = qrCode;
        if (body != null) {
            if (queryCode == null || queryCode.isBlank()) queryCode = body.get("bookingCode");
            if (queryCode == null || queryCode.isBlank()) queryCode = body.get("code");
            if (queryCode == null || queryCode.isBlank()) queryCode = body.get("qrCode");
        }
        return processLookup(user, queryCode, null, null);
    }

    private ApiResponse<BookingResponse> processLookup(
            com.cinemaai.booking.security.AuthenticatedUser user,
            String bookingCode,
            String code,
            String qrCode
    ) {
        String queryCode = bookingCode;
        if (queryCode == null || queryCode.isBlank()) queryCode = code;
        if (queryCode == null || queryCode.isBlank()) queryCode = qrCode;

        if (queryCode == null || queryCode.isBlank()) {
            throw new BadRequestException("Mã đặt vé hoặc mã QR không được để trống.");
        }

        String targetCode = extractBookingCode(queryCode);
        Booking booking = bookingRepository.findByBookingCode(targetCode)
                .or(() -> bookingSeatRepository.findByTicketCode(targetCode).map(BookingSeat::getBooking))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy thông tin đặt vé cho mã: " + targetCode));

        cinemaSecurityService.validateCinemaAccess(user, booking.getCinemaId(), true);

        try {
            ensureSeatTicketCodes(booking);
        } catch (Exception ex) {
            log.warn("Non-fatal: Failed to ensure seat ticket codes for booking {}: {}", booking.getBookingCode(), ex.getMessage());
        }

        return ApiResponse.success(BookingMapper.toResponse(booking), "Tìm thấy thông tin đặt vé");
    }

    @Operation(summary = "Xác nhận Check-in toàn bộ vé của booking vào rạp (Chống check-in lặp)")
    @PostMapping
    @Transactional
    public ApiResponse<BookingResponse> checkIn(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestBody Map<String, String> payload
    ) {
        String input = payload.get("bookingCode");
        if (input == null || input.isBlank()) input = payload.get("code");
        if (input == null || input.isBlank()) input = payload.get("qrCode");

        if (input == null || input.isBlank()) {
            throw new BadRequestException("Mã đặt vé hoặc mã QR không được để trống.");
        }

        String bookingCode = extractBookingCode(input);
        Booking booking = bookingRepository.findByBookingCode(bookingCode)
                .or(() -> bookingSeatRepository.findByTicketCode(bookingCode).map(BookingSeat::getBooking))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã đặt vé: " + bookingCode));

        cinemaSecurityService.validateCinemaAccess(user, booking.getCinemaId(), true);

        if (booking.getStatus() == BookingStatus.USED) {
            throw new ConflictException("Cảnh báo: Vé này đã được check-in vào lúc " + booking.getCheckedInAt());
        }

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Vé đã bị hủy, không thể check-in vào rạp.");
        }

        if (booking.getStatus() == BookingStatus.REFUNDED) {
            throw new BadRequestException("Vé đã được hoàn tiền, không thể check-in vào rạp.");
        }

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Vé chưa thanh toán. Trạng thái hiện tại: " + booking.getStatus());
        }

        LocalDateTime now = LocalDateTime.now();
        booking.setStatus(BookingStatus.USED);
        booking.setCheckedInAt(now);

        ensureSeatTicketCodes(booking);
        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                seat.setStatus(BookingSeatStatus.CHECKED_IN);
                seat.setCheckedInAt(now);
            }
        }

        bookingRepository.save(booking);
        return ApiResponse.success(BookingMapper.toResponse(booking), "Xác nhận Check-in thành công");
    }

    @Operation(summary = "Check-in theo từng ghế được chọn (Partial check-in)")
    @PostMapping("/seats")
    @Transactional
    public ApiResponse<BookingResponse> checkInSeats(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestBody Map<String, Object> payload
    ) {
        String bookingCode = (String) payload.get("bookingCode");
        if (bookingCode == null || bookingCode.isBlank()) {
            bookingCode = (String) payload.get("code");
        }
        if (bookingCode == null || bookingCode.isBlank()) {
            throw new BadRequestException("Mã đặt vé không được để trống.");
        }

        @SuppressWarnings("unchecked")
        List<String> ticketCodes = (List<String>) payload.get("ticketCodes");
        if (ticketCodes == null || ticketCodes.isEmpty()) {
            throw new BadRequestException("Vui lòng chọn ít nhất một vé ghế để check-in.");
        }

        String targetCode = extractBookingCode(bookingCode);
        Booking booking = bookingRepository.findByBookingCode(targetCode)
                .or(() -> bookingSeatRepository.findByTicketCode(targetCode).map(BookingSeat::getBooking))
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã đặt vé: " + targetCode));

        cinemaSecurityService.validateCinemaAccess(user, booking.getCinemaId(), true);

        if (booking.getStatus() == BookingStatus.CANCELLED) {
            throw new BadRequestException("Vé đã bị hủy, không thể check-in.");
        }
        if (booking.getStatus() == BookingStatus.REFUNDED) {
            throw new BadRequestException("Vé đã được hoàn tiền, không thể check-in.");
        }
        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.USED) {
            throw new BadRequestException("Vé chưa thanh toán. Trạng thái hiện tại: " + booking.getStatus());
        }

        ensureSeatTicketCodes(booking);
        LocalDateTime now = LocalDateTime.now();
        Set<String> targetSet = new HashSet<>(ticketCodes);
        boolean anyUpdated = false;

        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                String fullTicket = seat.getTicketCode();
                String shortSeatLabel = seat.getRowLabel() + seat.getSeatNumber();
                if (targetSet.contains(fullTicket) || targetSet.contains(shortSeatLabel)) {
                    seat.setStatus(BookingSeatStatus.CHECKED_IN);
                    seat.setCheckedInAt(now);
                    anyUpdated = true;
                }
            }
        }

        if (!anyUpdated) {
            throw new NotFoundException("Không tìm thấy ghế nào khớp với mã vé đã chọn.");
        }

        boolean allCheckedIn = booking.getSeats() != null && booking.getSeats().stream()
                .allMatch(s -> s.getStatus() == BookingSeatStatus.CHECKED_IN);
        if (allCheckedIn) {
            booking.setStatus(BookingStatus.USED);
            if (booking.getCheckedInAt() == null) {
                booking.setCheckedInAt(now);
            }
        }

        bookingRepository.save(booking);
        return ApiResponse.success(BookingMapper.toResponse(booking), "Check-in ghế thành công");
    }

    @Operation(summary = "Lấy danh sách vé đã thanh toán / check-in gần đây")
    @GetMapping("/recent")
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getRecentBookings(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestParam(defaultValue = "8") int limit
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, null, true);
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<Booking> list = bookingRepository.findRecentForCheckInByCinema(
                List.of(BookingStatus.PAID, BookingStatus.USED), enforcedCinemaId, pageable);
        List<BookingResponse> responses = list.stream().map(BookingMapper::toResponse).toList();
        return ApiResponse.success(responses, "Lấy danh sách vé gần đây thành công");
    }

    @Operation(summary = "Lấy danh sách vé theo suất chiếu")
    @GetMapping("/showtimes/{showtimeId}/bookings")
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getBookingsByShowtime(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @PathVariable Long showtimeId
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, null, true);
        List<Booking> list = bookingRepository.findByShowtimeId(showtimeId);
        if (enforcedCinemaId != null) {
            list = list.stream().filter(b -> enforcedCinemaId.equals(b.getCinemaId())).toList();
        }
        List<BookingResponse> responses = list.stream().map(BookingMapper::toResponse).toList();
        return ApiResponse.success(responses, "Lấy danh sách vé theo suất chiếu thành công");
    }

    @Operation(summary = "Tra cuu don bap nuoc cho Staff soat mon")
    @GetMapping("/food-orders/lookup")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> lookupFoodOrder(@RequestParam String code) {
        String cleanCode = extractFoodOrderCode(code);
        if (cleanCode == null || cleanCode.isBlank()) {
            throw new BadRequestException("Ma tra cuu bap nuoc khong duoc de trong");
        }

        java.util.Optional<FoodOrder> orderOpt = foodOrderRepository.findByFoodOrderCode(cleanCode);

        if (orderOpt.isEmpty()) {
            String bookingCode = extractBookingCode(code);
            java.util.Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                List<FoodOrder> linkedOrders = foodOrderRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
                if (!linkedOrders.isEmpty()) {
                    orderOpt = java.util.Optional.of(linkedOrders.get(0));
                } else if (booking.getFoodItems() != null && !booking.getFoodItems().isEmpty()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", booking.getId());
                    map.put("orderCode", booking.getBookingCode());
                    map.put("foodOrderCode", booking.getBookingCode());
                    map.put("bookingCode", booking.getBookingCode());
                    map.put("bookingId", booking.getId());
                    map.put("status", "USED".equalsIgnoreCase(String.valueOf(booking.getStatus())) ? "PICKED_UP" : "PAID");
                    map.put("customerName", booking.getCustomerNameSnapshot());
                    map.put("customerPhone", booking.getCustomerPhoneSnapshot());
                    map.put("cinemaId", booking.getCinemaId());
                    map.put("cinemaName", booking.getCinemaNameSnapshot());
                    map.put("cinemaAddress", booking.getCinemaNameSnapshot());
                    map.put("createdAt", booking.getCreatedAt());
                    map.put("paidAt", booking.getPaidAt());
                    java.math.BigDecimal total = java.math.BigDecimal.ZERO;
                    List<Map<String, Object>> itemsList = new ArrayList<>();
                    for (com.cinemaai.booking.entity.BookingFoodItem f : booking.getFoodItems()) {
                        Map<String, Object> itemMap = new HashMap<>();
                        itemMap.put("id", f.getId());
                        itemMap.put("productId", f.getProductId());
                        itemMap.put("isCombo", f.isCombo());
                        itemMap.put("name", f.getProductNameSnapshot());
                        itemMap.put("quantity", f.getQuantity());
                        itemMap.put("unitPrice", f.getUnitPrice());
                        itemMap.put("totalPrice", f.getLineTotal());
                        total = total.add(f.getLineTotal());
                        itemsList.add(itemMap);
                    }
                    map.put("totalAmount", total);
                    map.put("subtotal", total);
                    map.put("items", itemsList);
                    return ApiResponse.success(map, "Tim thay don bap nuoc tu ve xem phim");
                }
            }
        }

        FoodOrder order = orderOpt.orElseThrow(() -> new NotFoundException("Khong tim thay don bap nuoc: " + cleanCode));

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderCode", order.getFoodOrderCode());
        map.put("foodOrderCode", order.getFoodOrderCode());
        map.put("status", order.getStatus());
        map.put("totalAmount", order.getTotalAmount());
        map.put("subtotal", order.getSubtotal());
        map.put("paidAt", order.getPaidAt());
        map.put("createdAt", order.getCreatedAt());
        map.put("bookingId", order.getBookingId());
        map.put("cinemaId", order.getCinemaId());
        map.put("cinemaName", order.getCinemaName());
        map.put("cinemaAddress", order.getCinemaAddress());

        if (order.getBookingId() != null) {
            bookingRepository.findById(order.getBookingId()).ifPresent(b -> {
                map.put("bookingCode", b.getBookingCode());
                map.put("customerName", b.getCustomerNameSnapshot());
                map.put("customerPhone", b.getCustomerPhoneSnapshot());
                if (map.get("cinemaName") == null) map.put("cinemaName", b.getCinemaNameSnapshot());
                if (map.get("cinemaId") == null) map.put("cinemaId", b.getCinemaId());
            });
        }

        List<Map<String, Object>> itemsList = order.getItems() != null ? order.getItems().stream().map(item -> {
            Map<String, Object> itemMap = new HashMap<>();
            itemMap.put("id", item.getId());
            itemMap.put("productId", item.getProductId());
            itemMap.put("isCombo", item.isCombo());
            itemMap.put("name", item.getProductNameSnapshot());
            itemMap.put("quantity", item.getQuantity());
            itemMap.put("unitPrice", item.getUnitPrice());
            itemMap.put("totalPrice", item.getLineTotal());
            return itemMap;
        }).toList() : List.of();
        map.put("items", itemsList);

        return ApiResponse.success(map, "Tim thay don bap nuoc");
    }

    @Operation(summary = "Xac nhan giao mon cho khach (Staff Pick-up)")
    @PostMapping("/food-orders/pickup")
    @Transactional
    public ApiResponse<Map<String, Object>> pickUpFoodOrder(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) code = body.get("foodOrderCode");
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Ma don bap nuoc khong duoc de trong.");
        }

        String cleanCode = extractFoodOrderCode(code);
        java.util.Optional<FoodOrder> orderOpt = foodOrderRepository.findByFoodOrderCode(cleanCode);

        if (orderOpt.isEmpty()) {
            String bookingCode = extractBookingCode(code);
            java.util.Optional<Booking> bookingOpt = bookingRepository.findByBookingCode(bookingCode);
            if (bookingOpt.isPresent()) {
                Booking booking = bookingOpt.get();
                List<FoodOrder> linkedOrders = foodOrderRepository.findByBookingIdOrderByCreatedAtDesc(booking.getId());
                if (!linkedOrders.isEmpty()) {
                    orderOpt = java.util.Optional.of(linkedOrders.get(0));
                } else if (booking.getFoodItems() != null && !booking.getFoodItems().isEmpty()) {
                    Map<String, Object> map = new HashMap<>();
                    map.put("id", booking.getId());
                    map.put("orderCode", booking.getBookingCode());
                    map.put("foodOrderCode", booking.getBookingCode());
                    map.put("status", "PICKED_UP");
                    return ApiResponse.success(map, "Xac nhan giao mon bap nuoc thanh cong");
                }
            }
        }

        FoodOrder order = orderOpt.orElseThrow(() -> new NotFoundException("Khong tim thay don bap nuoc: " + cleanCode));

        if ("PICKED_UP".equalsIgnoreCase(order.getStatus())) {
            throw new ConflictException("Don bap nuoc nay da duoc giao cho khach truoc do.");
        }

        order.setStatus("PICKED_UP");
        foodOrderRepository.save(order);

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderCode", order.getFoodOrderCode());
        map.put("foodOrderCode", order.getFoodOrderCode());
        map.put("status", "PICKED_UP");
        return ApiResponse.success(map, "Xac nhan giao mon thanh cong");
    }

    @Operation(summary = "Lay danh sach don bap nuoc gan day tai rap cho Staff F&B")
    @GetMapping("/food-orders/recent")
    @Transactional(readOnly = true)
    public ApiResponse<List<Map<String, Object>>> getRecentFoodOrders(
            @org.springframework.security.core.annotation.AuthenticationPrincipal com.cinemaai.booking.security.AuthenticatedUser user,
            @RequestParam(defaultValue = "50") int limit
    ) {
        Long enforcedCinemaId = cinemaSecurityService.resolveEnforcedCinemaId(user, null, true);
        List<FoodOrder> orders = foodOrderRepository.findAll(
                org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt")
        );
        if (enforcedCinemaId != null) {
            orders = orders.stream()
                    .filter(o -> o.getCinemaId() == null || enforcedCinemaId.equals(o.getCinemaId()))
                    .toList();
        }
        if (orders.size() > limit) {
            orders = orders.subList(0, limit);
        }
        List<Map<String, Object>> responses = orders.stream().map(order -> {
            Map<String, Object> map = new HashMap<>();
            map.put("id", order.getId());
            map.put("orderCode", order.getFoodOrderCode());
            map.put("foodOrderCode", order.getFoodOrderCode());
            map.put("status", order.getStatus());
            map.put("totalAmount", order.getTotalAmount());
            map.put("subtotal", order.getSubtotal());
            map.put("paidAt", order.getPaidAt());
            map.put("createdAt", order.getCreatedAt());
            map.put("bookingId", order.getBookingId());
            map.put("cinemaId", order.getCinemaId());
            map.put("cinemaName", order.getCinemaName());
            map.put("cinemaAddress", order.getCinemaAddress());

            if (order.getBookingId() != null) {
                bookingRepository.findById(order.getBookingId()).ifPresent(b -> {
                    map.put("bookingCode", b.getBookingCode());
                    map.put("customerName", b.getCustomerNameSnapshot());
                    map.put("customerPhone", b.getCustomerPhoneSnapshot());
                    if (map.get("cinemaName") == null) map.put("cinemaName", b.getCinemaNameSnapshot());
                    if (map.get("cinemaId") == null) map.put("cinemaId", b.getCinemaId());
                });
            }

            List<Map<String, Object>> itemsList = order.getItems() != null ? order.getItems().stream().map(item -> {
                Map<String, Object> itemMap = new HashMap<>();
                itemMap.put("id", item.getId());
                itemMap.put("productId", item.getProductId());
                itemMap.put("isCombo", item.isCombo());
                itemMap.put("name", item.getProductNameSnapshot());
                itemMap.put("quantity", item.getQuantity());
                itemMap.put("unitPrice", item.getUnitPrice());
                itemMap.put("totalPrice", item.getLineTotal());
                return itemMap;
            }).toList() : List.of();
            map.put("items", itemsList);
            return map;
        }).toList();
        return ApiResponse.success(responses, "Lay danh sach don bap nuoc thanh cong");
    }
    private void ensureSeatTicketCodes(Booking booking) {
        if (booking.getQrCode() == null || booking.getQrCode().isBlank()) {
            booking.setQrCode("CINEMA:" + booking.getBookingCode() + ":" + booking.getId());
        }

        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                if (seat.getTicketCode() == null || seat.getTicketCode().isBlank()) {
                    String ticketCode = booking.getBookingCode() + "-" + seat.getRowLabel() + seat.getSeatNumber();
                    seat.setTicketCode(ticketCode);
                    seat.setQrCode("TICKET:" + ticketCode);
                }
            }
        }
        bookingRepository.save(booking);
    }

    private String extractBookingCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("CINEMA:")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 2) {
                return parts[1];
            }
        }
        if (trimmed.startsWith("CINEAI:")) {
            // Examples: CINEAI:BOOKING:BK123:..., CINEAI:SEAT:BK123-A1:...
            String[] parts = trimmed.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        if (trimmed.startsWith("TICKET:")) {
            return trimmed.substring(7);
        }
        return trimmed;
    }

    private String extractFoodOrderCode(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("CINEAI:FOOD:")) {
            String[] parts = trimmed.split(":");
            if (parts.length >= 3) {
                return parts[2];
            }
        }
        return trimmed;
    }
}
