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

    @Operation(summary = "Tra cứu thông tin vé để soát vé qua GET")
    @GetMapping("/lookup")
    @Transactional
    public ApiResponse<BookingResponse> lookupGet(
            @RequestParam(required = false) String bookingCode,
            @RequestParam(required = false) String code,
            @RequestParam(required = false) String qrCode
    ) {
        return processLookup(bookingCode, code, qrCode);
    }

    @Operation(summary = "Tra cứu thông tin vé để soát vé qua POST")
    @PostMapping("/lookup")
    @Transactional
    public ApiResponse<BookingResponse> lookupPost(
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
        return processLookup(queryCode, null, null);
    }

    private ApiResponse<BookingResponse> processLookup(String bookingCode, String code, String qrCode) {
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
    public ApiResponse<BookingResponse> checkIn(@RequestBody Map<String, String> payload) {
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

        if (booking.getStatus() == BookingStatus.USED) {
            throw new ConflictException("Cảnh báo: Vé này đã được check-in vào lúc " + booking.getCheckedInAt());
        }

        if (booking.getStatus() != BookingStatus.PAID) {
            throw new BadRequestException("Vé chưa thanh toán hoặc đã bị hủy. Trạng thái hiện tại: " + booking.getStatus());
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
    public ApiResponse<BookingResponse> checkInSeats(@RequestBody Map<String, Object> payload) {
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

        if (booking.getStatus() != BookingStatus.PAID && booking.getStatus() != BookingStatus.USED) {
            throw new BadRequestException("Vé chưa thanh toán hoặc đã bị hủy. Trạng thái hiện tại: " + booking.getStatus());
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
    public ApiResponse<List<BookingResponse>> getRecentBookings(@RequestParam(defaultValue = "8") int limit) {
        Pageable pageable = PageRequest.of(0, Math.min(Math.max(limit, 1), 50));
        List<Booking> list = bookingRepository.findRecentForCheckIn(
                List.of(BookingStatus.PAID, BookingStatus.USED), pageable);
        List<BookingResponse> responses = list.stream().map(BookingMapper::toResponse).toList();
        return ApiResponse.success(responses, "Lấy danh sách vé gần đây thành công");
    }

    @Operation(summary = "Lấy danh sách vé theo suất chiếu")
    @GetMapping("/showtimes/{showtimeId}/bookings")
    @Transactional(readOnly = true)
    public ApiResponse<List<BookingResponse>> getBookingsByShowtime(@PathVariable Long showtimeId) {
        List<Booking> list = bookingRepository.findByShowtimeId(showtimeId);
        List<BookingResponse> responses = list.stream().map(BookingMapper::toResponse).toList();
        return ApiResponse.success(responses, "Lấy danh sách vé theo suất chiếu thành công");
    }

    @Operation(summary = "Tra cứu đơn bắp nước cho Staff soát món")
    @GetMapping("/food-orders/lookup")
    @Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> lookupFoodOrder(@RequestParam String code) {
        String cleanCode = extractFoodOrderCode(code);
        FoodOrder order = foodOrderRepository.findByFoodOrderCode(cleanCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước: " + cleanCode));

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderCode", order.getFoodOrderCode());
        map.put("foodOrderCode", order.getFoodOrderCode());
        map.put("status", order.getStatus());
        map.put("totalAmount", order.getTotalAmount());
        map.put("subtotal", order.getSubtotal());
        map.put("paidAt", order.getPaidAt());
        map.put("bookingId", order.getBookingId());
        return ApiResponse.success(map, "Tìm thấy đơn bắp nước");
    }

    @Operation(summary = "Xác nhận giao món cho khách (Staff Pick-up)")
    @PostMapping("/food-orders/pickup")
    @Transactional
    public ApiResponse<Map<String, Object>> pickUpFoodOrder(@RequestBody Map<String, String> body) {
        String code = body.get("code");
        if (code == null || code.isBlank()) code = body.get("foodOrderCode");
        if (code == null || code.isBlank()) {
            throw new BadRequestException("Mã đơn bắp nước không được để trống.");
        }

        String cleanCode = extractFoodOrderCode(code);
        FoodOrder order = foodOrderRepository.findByFoodOrderCode(cleanCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy đơn bắp nước: " + cleanCode));

        if ("PICKED_UP".equalsIgnoreCase(order.getStatus())) {
            throw new ConflictException("Đơn bắp nước này đã được giao cho khách trước đó.");
        }

        order.setStatus("PICKED_UP");
        foodOrderRepository.save(order);

        Map<String, Object> map = new HashMap<>();
        map.put("id", order.getId());
        map.put("orderCode", order.getFoodOrderCode());
        map.put("foodOrderCode", order.getFoodOrderCode());
        map.put("status", "PICKED_UP");
        return ApiResponse.success(map, "Xác nhận giao món thành công");
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
