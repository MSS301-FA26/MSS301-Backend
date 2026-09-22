package com.sba301.cinemaai.enums;

public enum SeatType {
    SINGLE,
    COUPLE,
    NORMAL,
    STANDARD,
    VIP;

    public boolean isCouple() {
        return this == COUPLE;
    }

    public int getCapacity() {
        return this == COUPLE ? 2 : 1;
    }
}
