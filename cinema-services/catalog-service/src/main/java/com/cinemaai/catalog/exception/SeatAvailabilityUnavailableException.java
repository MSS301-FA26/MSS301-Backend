package com.cinemaai.catalog.exception;

/** Occupancy is unknown, not an authoritative empty set of bookings. */
public class SeatAvailabilityUnavailableException extends RuntimeException {
    public SeatAvailabilityUnavailableException() {
        super("Seat availability is temporarily unavailable");
    }
}
