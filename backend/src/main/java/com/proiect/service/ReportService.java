package com.proiect.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ExpenseAnalyticsService analyticsService;

    public String generateMonthlySummary(int year, int month) {
        log.info("Generating monthly summary for {}/{}", month, year);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1).minusDays(1);

        BigDecimal total = analyticsService.calculateTotal(from, to);
        Map<String, BigDecimal> byCategory = analyticsService.byCategory(from, to);

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Monthly Report for %s %d:\n", from.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH), year));
        summary.append(String.format("- Total Spent: %s RON\n", total));
        summary.append("- Breakdown by Category:\n");
        
        byCategory.forEach((cat, amount) -> 
            summary.append(String.format("  * %s: %s RON\n", cat, amount))
        );

        // Simple trend analysis for top category
        if (!byCategory.isEmpty()) {
            String topCategory = byCategory.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            summary.append(String.format("- Top Category: %s\n", topCategory));
            summary.append("- Trend: ").append(analyticsService.calculateTrend(topCategory, from, to));
        }

        return summary.toString();
    }
}
