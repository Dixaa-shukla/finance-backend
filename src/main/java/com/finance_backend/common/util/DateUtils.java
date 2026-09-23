package com.finance_backend.common.util;

import java.time.LocalDate;
import java.time.YearMonth;
import java.time.format.DateTimeFormatter;

public final class DateUtils {

    private DateUtils() {
    }

    public static final DateTimeFormatter DISPLAY_DATE_FORMAT = DateTimeFormatter.ofPattern("dd MMM yyyy");

    public static LocalDate startOfMonth(LocalDate date) {
        return YearMonth.from(date).atDay(1);
    }

    public static LocalDate endOfMonth(LocalDate date) {
        return YearMonth.from(date).atEndOfMonth();
    }

    public static String format(LocalDate date) {
        return date == null ? null : date.format(DISPLAY_DATE_FORMAT);
    }
}
