package com.proiect.service;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseTools {

    private final ExpenseAnalyticsService analyticsService;

    @Tool("Calculate total expenses for a date range")
    public String calculateTotal(String from, String to) {
        log.info("Tool called: calculateTotal from {} to {}", from, to);
        BigDecimal total = analyticsService.calculateTotal(LocalDate.parse(from), LocalDate.parse(to));
        return "Total expenses: " + total + " RON";
    }

    @Tool("Compare spending between family members for a date range")
    public String compareMembers(String from, String to) {
        log.info("Tool called: compareMembers from {} to {}", from, to);
        Map<String, BigDecimal> result = analyticsService.compareMembers(LocalDate.parse(from), LocalDate.parse(to));
        return "Spending by member: " + result.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " RON")
                .collect(Collectors.joining(", "));
    }

    @Tool("Detect spending anomalies above a threshold")
    public String detectAnomalies(String thresholdStr) {
        log.info("Tool called: detectAnomalies with threshold {}", thresholdStr);
        BigDecimal threshold = new BigDecimal(thresholdStr);
        return "Anomalies found: " + analyticsService.detectAnomalies(threshold).stream()
                .map(e -> e.getCategory() + " (" + e.getAmount() + " RON on " + e.getDate() + ")")
                .collect(Collectors.joining(", "));
    }

    @Tool("Get expense breakdown by category for a date range")
    public String byCategory(String from, String to) {
        log.info("Tool called: byCategory from {} to {}", from, to);
        Map<String, BigDecimal> result = analyticsService.byCategory(LocalDate.parse(from), LocalDate.parse(to));
        return "Breakdown by category: " + result.entrySet().stream()
                .map(e -> e.getKey() + ": " + e.getValue() + " RON")
                .collect(Collectors.joining(", "));
    }

    @Tool("Get expenses for a specific person in a date range")
    public String byPerson(String person, String from, String to) {
        log.info("Tool called: byPerson for {} from {} to {}", person, from, to);
        return "Expenses for " + person + ": " + analyticsService.findByPerson(person, LocalDate.parse(from), LocalDate.parse(to)).stream()
                .map(e -> e.getAmount() + " RON for " + e.getCategory() + " on " + e.getDate())
                .collect(Collectors.joining("; "));
    }

    @Tool("Compare expenses between two time periods")
    public String comparePeriods(String from1, String to1, String from2, String to2) {
        log.info("Tool called: comparePeriods");
        BigDecimal total1 = analyticsService.calculateTotal(LocalDate.parse(from1), LocalDate.parse(to1));
        BigDecimal total2 = analyticsService.calculateTotal(LocalDate.parse(from2), LocalDate.parse(to2));
        return "Period 1 (" + from1 + " to " + to1 + "): " + total1 + " RON. " +
               "Period 2 (" + from2 + " to " + to2 + "): " + total2 + " RON.";
    }

    @Tool("Get top N highest expenses")
    public String topExpenses(String limit) {
        log.info("Tool called: topExpenses with limit {}", limit);
        return "Top expenses: " + analyticsService.getTopExpenses(Integer.parseInt(limit)).stream()
                .map(e -> e.getAmount() + " RON (" + e.getCategory() + ") by " + e.getPerson() + " on " + e.getDate())
                .collect(Collectors.joining(", "));
    }

    @Tool("Calculate monthly average spending over the last N months")
    public String monthlyAverage(String months) {
        log.info("Tool called: monthlyAverage for last {} months", months);
        BigDecimal avg = analyticsService.calculateMonthlyAverage(Integer.parseInt(months));
        return "Monthly average for the last " + months + " months: " + avg + " RON";
    }

    @Tool("Describe spending trend for a category in a date range compared to the previous period")
    public String describeTrend(String category, String from, String to) {
        log.info("Tool called: describeTrend for {} from {} to {}", category, from, to);
        return analyticsService.calculateTrend(category, LocalDate.parse(from), LocalDate.parse(to));
    }

    @Tool("Get a short visual description of the trend for frontend charts")
    public String getVisualDescription(String category, String from, String to) {
        log.info("Tool called: getVisualDescription for {} from {} to {}", category, from, to);
        String trend = analyticsService.calculateTrend(category, LocalDate.parse(from), LocalDate.parse(to));
        
        if (trend.contains("increased")) {
            String percent = extractPercentage(trend);
            return "Trendul arată o creștere de " + percent + "% pentru " + category;
        } else if (trend.contains("decreased")) {
            String percent = extractPercentage(trend);
            return "Trendul arată o scădere de " + percent + "% pentru " + category;
        }
        return "Trend stabil pentru " + category;
    }

    private String extractPercentage(String trend) {
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("(\\d+(\\.\\d+)?)%").matcher(trend);
        return m.find() ? m.group(1) : "0";
    }
}
