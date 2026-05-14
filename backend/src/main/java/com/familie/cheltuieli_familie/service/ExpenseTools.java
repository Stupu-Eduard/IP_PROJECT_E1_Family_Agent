package com.familie.cheltuieli_familie.service;

import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Component
@Slf4j
@RequiredArgsConstructor
public class ExpenseTools {

    private static final Pattern PERCENTAGE_PATTERN = Pattern.compile("(\\d{1,10}(\\.\\d{1,10})?)%");
    private static final int MAX_TREND_LENGTH = 1000;
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_CATEGORY = "category";
    private static final String KEY_DATE = "date";
    private static final String KEY_PERSON = "person";
    private static final String KEY_LOCATION = "location";
    private static final String KEY_DESCRIPTION = "description";

    private final ExpenseAnalyticsService analyticsService;

    @Tool("Calculate total expenses for a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String calculateTotal(String from, String to) {
        try {
            log.info("Tool called: calculateTotal from {} to {}", from, to);
            BigDecimal total = analyticsService.calculateTotal(LocalDate.parse(from), LocalDate.parse(to));
            return "Total expenses: " + total + " RON";
        } catch (Exception e) {
            log.error("Error in calculateTotal: {}", e.getMessage());
            return "Error calculating total: " + e.getMessage();
        }
    }

    @Tool("Compare spending between family members for a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String compareMembers(String from, String to) {
        try {
            log.info("Tool called: compareMembers from {} to {}", from, to);
            Map<String, BigDecimal> result = analyticsService.compareMembers(LocalDate.parse(from), LocalDate.parse(to));
            if (result.isEmpty()) return "No spending data found for the specified period.";
            return "Spending by member: " + result.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue() + " RON")
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("Error in compareMembers: {}", e.getMessage());
            return "Error comparing members: " + e.getMessage();
        }
    }

    @Tool("Detect spending anomalies above a threshold amount in RON")
    public String detectAnomalies(String thresholdStr) {
        try {
            log.info("Tool called: detectAnomalies with threshold {}", thresholdStr);
            BigDecimal threshold = new BigDecimal(thresholdStr);
            var anomalies = analyticsService.detectAnomalies(threshold);
            if (anomalies.isEmpty()) return "No anomalies found above " + threshold + " RON.";
            return "Anomalies found: " + anomalies.stream()
                    .map(e -> e.get(KEY_CATEGORY) + " (" + e.get(KEY_AMOUNT) + " RON on " + e.get(KEY_DATE) + ")")
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("Error in detectAnomalies: {}", e.getMessage());
            return "Error detecting anomalies: " + e.getMessage();
        }
    }

    @Tool("Get expense breakdown by category for a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String byCategory(String from, String to) {
        try {
            log.info("Tool called: byCategory from {} to {}", from, to);
            Map<String, BigDecimal> result = analyticsService.byCategory(LocalDate.parse(from), LocalDate.parse(to));
            if (result.isEmpty()) return "No expenses found for the specified period.";
            return "Breakdown by category: " + result.entrySet().stream()
                    .map(e -> e.getKey() + ": " + e.getValue() + " RON")
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("Error in byCategory: {}", e.getMessage());
            return "Error getting category breakdown: " + e.getMessage();
        }
    }

    @Tool("Get expenses for a specific person in a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String byPerson(String person, String from, String to) {
        try {
            log.info("Tool called: byPerson for {} from {} to {}", person, from, to);
            var expenses = analyticsService.findByPerson(person, LocalDate.parse(from), LocalDate.parse(to));
            if (expenses.isEmpty()) return "No expenses found for " + person + " in the specified period.";
            return "Expenses for " + person + ": " + expenses.stream()
                    .map(e -> e.get(KEY_AMOUNT) + " RON for " + e.get(KEY_CATEGORY) + " on " + e.get(KEY_DATE))
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            log.error("Error in byPerson: {}", e.getMessage());
            return "Error finding expenses for person: " + e.getMessage();
        }
    }

    @Tool("Compare expenses between two time periods. All dates must be in ISO format YYYY-MM-DD.")
    public String comparePeriods(String from1, String to1, String from2, String to2) {
        try {
            log.info("Tool called: comparePeriods");
            BigDecimal total1 = analyticsService.calculateTotal(LocalDate.parse(from1), LocalDate.parse(to1));
            BigDecimal total2 = analyticsService.calculateTotal(LocalDate.parse(from2), LocalDate.parse(to2));
            return "Period 1 (" + from1 + " to " + to1 + "): " + total1 + " RON. " +
                   "Period 2 (" + from2 + " to " + to2 + "): " + total2 + " RON.";
        } catch (Exception e) {
            log.error("Error in comparePeriods: {}", e.getMessage());
            return "Error comparing periods: " + e.getMessage();
        }
    }

    @Tool("Get top N highest expenses")
    public String topExpenses(String limit) {
        try {
            log.info("Tool called: topExpenses with limit {}", limit);
            var expenses = analyticsService.getTopExpenses(Integer.parseInt(limit));
            if (expenses.isEmpty()) return "No expenses found.";
            return "Top expenses: " + expenses.stream()
                    .map(e -> e.get(KEY_AMOUNT) + " RON (" + e.get(KEY_CATEGORY) + ") by " + e.get(KEY_PERSON) + " on " + e.get(KEY_DATE))
                    .collect(Collectors.joining(", "));
        } catch (Exception e) {
            log.error("Error in topExpenses: {}", e.getMessage());
            return "Error getting top expenses: " + e.getMessage();
        }
    }

    @Tool("Calculate monthly average spending over the last N months")
    public String monthlyAverage(String months) {
        try {
            log.info("Tool called: monthlyAverage for last {} months", months);
            BigDecimal avg = analyticsService.calculateMonthlyAverage(Integer.parseInt(months));
            return "Monthly average for the last " + months + " months: " + avg + " RON";
        } catch (Exception e) {
            log.error("Error in monthlyAverage: {}", e.getMessage());
            return "Error calculating monthly average: " + e.getMessage();
        }
    }

    @Tool("Describe spending trend for a category in a date range compared to the previous period. Dates must be in ISO format YYYY-MM-DD.")
    public String describeTrend(String category, String from, String to) {
        try {
            log.info("Tool called: describeTrend for {} from {} to {}", category, from, to);
            return analyticsService.calculateTrend(category, LocalDate.parse(from), LocalDate.parse(to));
        } catch (Exception e) {
            log.error("Error in describeTrend: {}", e.getMessage());
            return "Error calculating trend: " + e.getMessage();
        }
    }

    @Tool("Get a short visual description of the trend for frontend charts")
    public String getVisualDescription(String category, String from, String to) {
        try {
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
        } catch (Exception e) {
            log.error("Error in getVisualDescription: {}", e.getMessage());
            return "Error getting visual description: " + e.getMessage();
        }
    }

    @Tool("Get expenses for a specific category in a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String byCategoryDetailed(String category, String from, String to) {
        try {
            log.info("Tool called: byCategoryDetailed for {} from {} to {}", category, from, to);
            var expenses = analyticsService.findByCategory(category, LocalDate.parse(from), LocalDate.parse(to));
            if (expenses.isEmpty()) return "No expenses found for category '" + category + "' in the specified period.";
            return "Expenses for '" + category + "': " + expenses.stream()
                    .map(e -> e.get(KEY_AMOUNT) + " RON at " + e.get(KEY_LOCATION) + " on " + e.get(KEY_DATE) + " (" + e.get(KEY_DESCRIPTION) + ")")
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            log.error("Error in byCategoryDetailed: {}", e.getMessage());
            return "Error finding category expenses: " + e.getMessage();
        }
    }

    @Tool("Get expenses for a specific location in a date range. Dates must be in ISO format YYYY-MM-DD.")
    public String byLocation(String location, String from, String to) {
        try {
            log.info("Tool called: byLocation for {} from {} to {}", location, from, to);
            var expenses = analyticsService.findByLocation(location, LocalDate.parse(from), LocalDate.parse(to));
            if (expenses.isEmpty()) return "No expenses found for location '" + location + "' in the specified period.";
            return "Expenses at '" + location + "': " + expenses.stream()
                    .map(e -> e.get(KEY_AMOUNT) + " RON for " + e.get(KEY_CATEGORY) + " on " + e.get(KEY_DATE) + " (" + e.get(KEY_DESCRIPTION) + ")")
                    .collect(Collectors.joining("; "));
        } catch (Exception e) {
            log.error("Error in byLocation: {}", e.getMessage());
            return "Error finding location expenses: " + e.getMessage();
        }
    }

    private String extractPercentage(String trend) {
        if (trend == null || trend.length() > MAX_TREND_LENGTH) {
            return "0";
        }
        Matcher m = PERCENTAGE_PATTERN.matcher(trend);
        return m.find() ? m.group(1) : "0";
    }
}
