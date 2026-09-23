package com.cinemaai.booking.controller;

import com.cinemaai.booking.dto.response.ApiResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.enums.BookingSeatStatus;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.exception.BadRequestException;
import com.cinemaai.booking.exception.ConflictException;
import com.cinemaai.booking.exception.NotFoundException;
import com.cinemaai.booking.mapper.BookingMapper;
import com.cinemaai.booking.repository.BookingRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import java.time.LocalDateTime;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/staff/check-in")
@RequiredArgsConstructor
@Tag(name = "Staff Check-In", description = "Quét mã QR và soát vé vào rạp")
public class StaffCheckInController {

    private final BookingRepository bookingRepository;

    @Operation(summary = "Tra cứu thông tin vé để soát vé")
    @RequestMapping(value = "/lookup", method = {RequestMethod.GET, RequestMethod.POST})
    public ApiResponse<BookingResponse> lookup(
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

        if (queryCode == null || queryCode.isBlank()) {
            throw new BadRequestException("Mã đặt vé hoặc mã QR không được để trống.");
        }

        String targetCode = extractBookingCode(queryCode);
        Booking booking = bookingRepository.findByBookingCode(targetCode)
                .orElseThrow(() -> new NotFoundException("Không tìm thấy mã đặt vé: " + targetCode));
        return ApiResponse.success(BookingMapper.toResponse(booking));
    }

    @Operation(summary = "Xác nhận Check-in vé vào rạp (Chống check-in lặp)")
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

        if (booking.getSeats() != null) {
            for (BookingSeat seat : booking.getSeats()) {
                seat.setStatus(BookingSeatStatus.CHECKED_IN);
                seat.setCheckedInAt(now);
            }
        }

        bookingRepository.save(booking);
        return ApiResponse.success(BookingMapper.toResponse(booking), "Xác nhận Check-in thành công");
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
        return trimmed;
    }
}
