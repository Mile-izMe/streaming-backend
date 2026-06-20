package com.melody.melody_stream.core.converter;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.persistence.AttributeConverter;
import jakarta.persistence.Converter;

@Converter
public class StringArrayConverter implements AttributeConverter<String[], String> {

    private static final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public String convertToDatabaseColumn(String[] attribute) {
        if (attribute == null) return null;
        try {
            return objectMapper.writeValueAsString(attribute);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert String[] to JSON", e);
        }
    }

    @Override
    public String[] convertToEntityAttribute(String dbData) {
        if (dbData == null) return null;
        try {
            return objectMapper.readValue(dbData, String[].class);
        } catch (Exception e) {
            throw new RuntimeException("Failed to convert JSON to String[]", e);
        }
    }
}
