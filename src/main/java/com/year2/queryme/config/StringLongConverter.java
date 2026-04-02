package com.year2.queryme.config;

import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringLongConverter implements AttributeConverter<Long, Object> {

    @Override
    public Object convertToDatabaseColumn(Long attribute) {
        return attribute;
    }

    @Override
    public Long convertToEntityAttribute(Object dbData) {
        if (dbData == null) {
            return null;
        }
        if (dbData instanceof Number) {
            return ((Number) dbData).longValue();
        }
        // Handle the case where the DB returns a padded string or binary
        String str = dbData.toString().trim();
        // Remove null characters (\0) which are causing the JDBC error
        str = str.replace("\u0000", "");
        
        try {
            return Long.parseLong(str);
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
