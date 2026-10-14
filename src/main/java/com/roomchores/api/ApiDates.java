package com.roomchores.api;

import jakarta.ws.rs.BadRequestException;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeParseException;

final class ApiDates {

    private ApiDates() {
    }

    static LocalDate date(String value, LocalDate fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return LocalDate.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid date '" + value + "', expected YYYY-MM-DD");
        }
    }

    static YearMonth month(String value, YearMonth fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        try {
            return YearMonth.parse(value);
        } catch (DateTimeParseException e) {
            throw new BadRequestException("Invalid month '" + value + "', expected YYYY-MM");
        }
    }
}
