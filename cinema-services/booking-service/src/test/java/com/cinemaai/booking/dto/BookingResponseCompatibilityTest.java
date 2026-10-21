package com.cinemaai.booking.dto;

import static org.assertj.core.api.Assertions.assertThat;

import com.cinemaai.booking.dto.response.BookingProductResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.BookingSeatResponse;
import com.cinemaai.booking.dto.response.BookingTicketResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.enums.BookingStatus;
import com.cinemaai.booking.mapper.BookingMapper;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mapstruct.factory.Mappers;

class BookingResponseCompatibilityTest {

    private final ObjectMapper json = new ObjectMapper().findAndRegisterModules();

    @Test
    void builderUsesNamedAssignmentsAndPreservesNestedCollections() {
        BookingSeatResponse seat = new BookingSeatResponse(
                1L, 11L, 21L, "A", 1, "A1", null, BigDecimal.TEN,
                null, null, null, null, null);
        BookingTicketResponse ticket = new BookingTicketResponse(
                2L, 11L, null, null, 1, BigDecimal.TEN, BigDecimal.TEN);
        BookingProductResponse food = new BookingProductResponse(
                3L, 31L, false, "Popcorn", 2, BigDecimal.ONE, BigDecimal.valueOf(2));

        BookingResponse response = BookingResponse.builder()
                .id(7L)
                .bookingCode("BK-7")
                .movieTitle("Movie")
                .cinemaId(41L)
                .status(BookingStatus.PAID)
                .seats(List.of(seat))
                .tickets(List.of(ticket))
                .foods(List.of(food))
                .refundReason(null)
                .build();

        assertThat(response.id()).isEqualTo(7L);
        assertThat(response.bookingCode()).isEqualTo("BK-7");
        assertThat(response.cinemaId()).isEqualTo(41L);
        assertThat(response.status()).isEqualTo(BookingStatus.PAID);
        assertThat(response.seats()).containsExactly(seat);
        assertThat(response.tickets()).containsExactly(ticket);
        assertThat(response.foods()).containsExactly(food);
        assertThat(response.holdExpiresAt()).isNull();
        assertThat(response.refundReason()).isNull();
    }

    @Test
    void canonicalAndExistingOverloadedConstructorsRemainAvailable() {
        List<Integer> parameterCounts = Arrays.stream(BookingResponse.class.getDeclaredConstructors())
                .map(constructor -> constructor.getParameterCount())
                .sorted()
                .toList();

        assertThat(parameterCounts).containsExactly(30, 31, 36);
    }

    @Test
    void jsonContractRetainsLegacyFieldsAndSupportsRoundTrip() throws Exception {
        LocalDateTime showtime = LocalDateTime.of(2026, 10, 22, 19, 30);
        BookingResponse response = BookingResponse.builder()
                .id(7L)
                .bookingCode("BK-7")
                .movieTitle("Movie")
                .movieTitleSnapshot("Movie")
                .posterUrl("poster.jpg")
                .cinemaName("Cinema")
                .roomName("Room 1")
                .showtimeStart(showtime)
                .subtotal(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.TEN)
                .totalAmount(BigDecimal.valueOf(90))
                .status(BookingStatus.PAID)
                .seats(List.of())
                .tickets(List.of())
                .foods(List.of())
                .build();

        String serialized = json.writeValueAsString(response);
        JsonNode tree = json.readTree(serialized);
        BookingResponse restored = json.readValue(serialized, BookingResponse.class);

        assertThat(tree.has("foods")).isTrue();
        assertThat(tree.has("movieTitleSnapshot")).isTrue();
        assertThat(tree.has("userEmail")).isTrue();
        assertThat(tree.has("showtimeEnd")).isTrue();
        assertThat(tree.has("refundReason")).isTrue();
        assertThat(restored).isEqualTo(response);
    }

    @Test
    void mapStructPreservesSnapshotAliasesAndOptionalFields() {
        LocalDateTime start = LocalDateTime.of(2026, 10, 22, 19, 30);
        LocalDateTime end = start.plusHours(2);
        Booking booking = Booking.builder()
                .id(7L)
                .bookingCode("BK-7")
                .userId(8L)
                .showtimeId(9L)
                .movieId(10L)
                .movieTitleSnapshot("Movie snapshot")
                .moviePosterSnapshot("poster.jpg")
                .cinemaNameSnapshot("Cinema snapshot")
                .cinemaId(11L)
                .roomNameSnapshot("Room snapshot")
                .showtimeStartSnapshot(start)
                .showtimeEndSnapshot(end)
                .customerNameSnapshot("Customer")
                .customerPhoneSnapshot("0900000000")
                .customerEmailSnapshot("customer@example.com")
                .subtotal(BigDecimal.valueOf(100))
                .discountAmount(BigDecimal.TEN)
                .loyaltyPointsRedeemed(10)
                .totalAmount(BigDecimal.valueOf(90))
                .status(BookingStatus.PAID)
                .refundReason("reason")
                .build();

        BookingResponse response = Mappers.getMapper(BookingMapper.class).toResponse(booking);

        assertThat(response.movieTitle()).isEqualTo("Movie snapshot");
        assertThat(response.movieTitleSnapshot()).isEqualTo("Movie snapshot");
        assertThat(response.posterUrl()).isEqualTo("poster.jpg");
        assertThat(response.moviePosterSnapshot()).isEqualTo("poster.jpg");
        assertThat(response.cinemaName()).isEqualTo("Cinema snapshot");
        assertThat(response.cinemaNameSnapshot()).isEqualTo("Cinema snapshot");
        assertThat(response.cinemaId()).isEqualTo(11L);
        assertThat(response.roomName()).isEqualTo("Room snapshot");
        assertThat(response.roomNameSnapshot()).isEqualTo("Room snapshot");
        assertThat(response.showtimeStart()).isEqualTo(start);
        assertThat(response.showtimeStartSnapshot()).isEqualTo(start);
        assertThat(response.showtimeEnd()).isEqualTo(end);
        assertThat(response.customerName()).isEqualTo("Customer");
        assertThat(response.customerPhone()).isEqualTo("0900000000");
        assertThat(response.userEmail()).isEqualTo("customer@example.com");
        assertThat(response.foods()).isEmpty();
        assertThat(response.seats()).isEmpty();
        assertThat(response.tickets()).isEmpty();
        assertThat(response.refundReason()).isEqualTo("reason");
    }
}
