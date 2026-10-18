package com.cinemaai.booking.mapper;

import com.cinemaai.booking.dto.response.BookingFoodResponse;
import com.cinemaai.booking.dto.response.BookingResponse;
import com.cinemaai.booking.dto.response.BookingSeatResponse;
import com.cinemaai.booking.dto.response.BookingTicketResponse;
import com.cinemaai.booking.entity.Booking;
import com.cinemaai.booking.entity.BookingFoodItem;
import com.cinemaai.booking.entity.BookingSeat;
import com.cinemaai.booking.entity.BookingTicket;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.factory.Mappers;

@Mapper(componentModel = "spring")
public interface BookingMapper {

    BookingMapper INSTANCE = Mappers.getMapper(BookingMapper.class);

    @Mapping(target = "movieTitle", source = "movieTitleSnapshot")
    @Mapping(target = "posterUrl", source = "moviePosterSnapshot")
    @Mapping(target = "cinemaName", source = "cinemaNameSnapshot")
    @Mapping(target = "roomName", source = "roomNameSnapshot")
    @Mapping(target = "showtimeStart", source = "showtimeStartSnapshot")
    @Mapping(target = "customerName", source = "customerNameSnapshot")
    @Mapping(target = "customerPhone", source = "customerPhoneSnapshot")
    @Mapping(target = "userEmail", source = "customerEmailSnapshot")
    @Mapping(target = "showtimeEnd", source = "showtimeEndSnapshot")
    @Mapping(target = "foods", source = "foodItems")
    BookingResponse toResponse(Booking booking);

    @Mapping(target = "seatLabel", expression = "java(seat.getRowLabel() + seat.getSeatNumber())")
    BookingSeatResponse toSeatResponse(BookingSeat seat);

    BookingTicketResponse toTicketResponse(BookingTicket ticket);

    @Mapping(target = "isCombo", source = "combo")
    BookingFoodResponse toFoodResponse(BookingFoodItem food);
}
