package com.familie.cheltuieli_familie.util;

import lombok.extern.slf4j.Slf4j;

import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;

@Slf4j
public class DateRangeUtil {

    private DateRangeUtil() {}

    public static LocalDate[] parseDateRange(String dateRange) {
        if (dateRange == null || dateRange.isBlank()) {
            return new LocalDate[]{null, null};
        }

        LocalDate now = LocalDate.now();

        switch (dateRange.toLowerCase().replace(" ", "_")) {
            case "today":
                return new LocalDate[]{now, now};
            case "yesterday":
                LocalDate yesterday = now.minusDays(1);
                return new LocalDate[]{yesterday, yesterday};
            case "this_week":
                return new LocalDate[]{
                        now.with(TemporalAdjusters.previousOrSame(java.time.DayOfWeek.MONDAY)),
                        now
                };
            case "this_month":
                return new LocalDate[]{
                        now.withDayOfMonth(1),
                        now.with(TemporalAdjusters.lastDayOfMonth())
                };
            case "last_month":
                LocalDate lastMonth = now.minusMonths(1);
                return new LocalDate[]{
                        lastMonth.withDayOfMonth(1),
                        lastMonth.with(TemporalAdjusters.lastDayOfMonth())
                };
            case "last_3_months":
                return new LocalDate[]{
                        now.minusMonths(3).withDayOfMonth(1),
                        now
                };
            case "last_6_months":
                return new LocalDate[]{
                        now.minusMonths(6).withDayOfMonth(1),
                        now
                };
            case "this_year":
                return new LocalDate[]{
                        now.withDayOfYear(1),
                        now
                };
            case "last_year":
                LocalDate lastYear = now.minusYears(1);
                return new LocalDate[]{
                        lastYear.withDayOfYear(1),
                        lastYear.with(TemporalAdjusters.lastDayOfYear())
                };
            default:
                // Try to parse as ISO range: "2024-01-01 to 2024-03-31"
                String[] parts = dateRange.split("to|—|–|-");
                if (parts.length >= 2) {
                    try {
                        LocalDate from = LocalDate.parse(parts[0].trim());
                        LocalDate to = LocalDate.parse(parts[1].trim());
                        return new LocalDate[]{from, to};
                    } catch (Exception e) {
                        log.warn("Could not parse date range '{}': {}", dateRange, e.getMessage());
                    }
                }
                return new LocalDate[]{null, null};
        }
    }
}
