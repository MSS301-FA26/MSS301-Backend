package com.sba301.cinemaai.enums;

import com.sba301.cinemaai.exception.BadRequestException;
import java.util.Arrays;

public enum AgeRating {
    P("P", 0),
    K("K", 0),
    T13("13+", 13),
    T16("16+", 16),
    T18("18+", 18);

    private final String label;
    private final int minimumAge;

    AgeRating(String label, int minimumAge) {
        this.label = label;
        this.minimumAge = minimumAge;
    }

    public String getLabel() {
        return label;
    }

    public int getMinimumAge() {
        return minimumAge;
    }

    public boolean allowsAge(int age) {
        return age >= minimumAge;
    }

    public static AgeRating from(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toUpperCase().replace(" ", "");
        return Arrays.stream(values())
                .filter(ageRating -> ageRating.name().equals(normalized)
                        || ageRating.label.equalsIgnoreCase(normalized)
                        || normalized.equals("T" + ageRating.minimumAge)
                        || normalized.equals(ageRating.minimumAge + "+"))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Age rating is invalid"));
    }
}
