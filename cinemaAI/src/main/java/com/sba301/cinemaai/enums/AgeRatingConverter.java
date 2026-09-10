package com.sba301.cinemaai.enums;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class AgeRatingConverter implements AttributeConverter<AgeRating, String> {

    @Override
    public String convertToDatabaseColumn(AgeRating attribute) {
        return attribute == null ? null : attribute.getLabel();
    }

    @Override
    public AgeRating convertToEntityAttribute(String dbData) {
        return dbData == null || dbData.isBlank() ? null : AgeRating.from(dbData);
    }
}
