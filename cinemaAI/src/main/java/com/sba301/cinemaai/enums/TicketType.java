package com.sba301.cinemaai.enums;

public enum TicketType {
    ADULT(18, 59),
    CHILD(0, 12),
    STUDENT(13, 25);

    private final int minimumAge;
    private final int maximumAge;

    TicketType(int minimumAge, int maximumAge) {
        this.minimumAge = minimumAge;
        this.maximumAge = maximumAge;
    }

    public boolean allowsAge(int age) {
        return age >= minimumAge && age <= maximumAge;
    }

    public int getMinimumAge() {
        return minimumAge;
    }

    public int getMaximumAge() {
        return maximumAge;
    }
}
