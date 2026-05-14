package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.config.LlmConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.Locale;
import java.util.Map;

@Service
@Slf4j
@RequiredArgsConstructor
public class ReportService {

    private final ExpenseAnalyticsService analyticsService;
    private final LlmConfig.ReportAssistant reportAssistant;

    public String generateMonthlySummary(int year, int month) {
        log.info("Generating monthly summary for {}/{}", month, year);
        LocalDate from = LocalDate.of(year, month, 1);
        LocalDate to = from.plusMonths(1).minusDays(1);

        BigDecimal total = analyticsService.calculateTotal(from, to);
        Map<String, BigDecimal> byCategory = analyticsService.byCategory(from, to);

        StringBuilder summary = new StringBuilder();
        summary.append(String.format("Monthly Report for %s %d:%n", from.getMonth().getDisplayName(TextStyle.FULL, Locale.ENGLISH), year));
        summary.append(String.format("- Total Spent: %s RON%n", total));
        summary.append("- Breakdown by Category:\n");

        byCategory.forEach((cat, amount) ->
            summary.append(String.format("  * %s: %s RON%n", cat, amount))
        );

        if (!byCategory.isEmpty()) {
            String topCategory = byCategory.entrySet().stream()
                    .max(Map.Entry.comparingByValue())
                    .get().getKey();
            summary.append(String.format("- Top Category: %s%n", topCategory));
            summary.append("- Trend: ").append(analyticsService.calculateTrend(topCategory, from, to));
        }

        return summary.toString();
    }

    public String generateNarrativeReport(int year, int month) {
        log.info("Generating narrative report for {}/{}", month, year);
        String rawSummary = generateMonthlySummary(year, month);
        return reportAssistant.generateReport(rawSummary);
    }
}
