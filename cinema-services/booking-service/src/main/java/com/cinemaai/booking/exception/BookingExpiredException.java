package com.cinemaai.booking.exception;

public class BookingExpiredException extends BadRequestException {
    public BookingExpiredException(String message) {
        super(message);
    }
}
