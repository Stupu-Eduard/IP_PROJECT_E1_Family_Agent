package com.familie.cheltuieli_familie.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
@Slf4j
@RequiredArgsConstructor
public class ExpenseAnalyticsService {

    private final JdbcTemplate jdbcTemplate;

    private static final String KEY_CATEGORY = "category";
    private static final String KEY_TOTAL = "total";
    private static final String KEY_PERSON = "person";

    // Actual DB schema uses foreign keys with JOINs for names
    private static final String BASE_SQL =
        "SELECT e.id, e.amount, e.description, e.expense_date as date, " +
        "c.name as category, l.store as location, u.name as person, " +
        "e.currency, e.source_type, e.raw_input " +
        "FROM expenses e " +
        "LEFT JOIN categories c ON e.category_id = c.id " +
        "LEFT JOIN locations l ON e.location_id = l.id " +
        "LEFT JOIN users u ON e.user_id = u.id ";

    public List<Map<String, Object>> findExpenses(LocalDate from, LocalDate to) {
        log.info("Finding expenses from {} to {}", from, to);
        String sql = BASE_SQL + " WHERE e.expense_date >= ? AND e.expense_date <= ? ORDER BY e.expense_date DESC";
        return jdbcTemplate.queryForList(sql, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public BigDecimal calculateTotal(LocalDate from, LocalDate to) {
        log.info("Calculating total expenses from {} to {}", from, to);
        String sql = "SELECT COALESCE(SUM(amount), 0) FROM expenses WHERE expense_date >= ? AND expense_date <= ?";
        return jdbcTemplate.queryForObject(sql, BigDecimal.class, from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public Map<String, BigDecimal> byCategory(LocalDate from, LocalDate to) {
        log.info("Calculating expenses by category from {} to {}", from, to);
        String sql = """
            SELECT c.name as category_name, SUM(e.amount) as total
            FROM expenses e
            LEFT JOIN categories c ON e.category_id = c.id
            WHERE e.expense_date >= ? AND e.expense_date <= ?
            GROUP BY c.name
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> Map.of(
                KEY_CATEGORY, rs.getString("category_name"),
                KEY_TOTAL, rs.getBigDecimal(KEY_TOTAL)
        ), from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        .stream()
        .filter(m -> m.get(KEY_CATEGORY) != null)
        .collect(Collectors.toMap(
                m -> (String) m.get(KEY_CATEGORY),
                m -> (BigDecimal) m.get(KEY_TOTAL),
                BigDecimal::add
        ));
    }

    public Map<String, BigDecimal> compareMembers(LocalDate from, LocalDate to) {
        log.info("Comparing expenses between members from {} to {}", from, to);
        String sql = """
            SELECT u.name as person_name, SUM(e.amount) as total
            FROM expenses e
            LEFT JOIN users u ON e.user_id = u.id
            WHERE e.expense_date >= ? AND e.expense_date <= ?
            GROUP BY u.name
            """;
        return jdbcTemplate.query(sql, (rs, rowNum) -> Map.of(
                KEY_PERSON, rs.getString("person_name"),
                KEY_TOTAL, rs.getBigDecimal(KEY_TOTAL)
        ), from.atStartOfDay(), to.plusDays(1).atStartOfDay())
        .stream()
        .filter(m -> m.get(KEY_PERSON) != null)
        .collect(Collectors.toMap(
                m -> (String) m.get(KEY_PERSON),
                m -> (BigDecimal) m.get(KEY_TOTAL),
                BigDecimal::add
        ));
    }

    public List<Map<String, Object>> detectAnomalies(BigDecimal threshold) {
        log.info("Detecting expense anomalies above threshold: {}", threshold);
        String sql = BASE_SQL + " WHERE e.amount > ? ORDER BY e.amount DESC";
        return jdbcTemplate.queryForList(sql, threshold);
    }

    public List<Map<String, Object>> findByPerson(String person, LocalDate from, LocalDate to) {
        log.info("Fetching expenses for person: {} from {} to {}", person, from, to);
        String sql = BASE_SQL + """
            WHERE u.name ILIKE ?
            AND e.expense_date >= ? AND e.expense_date <= ?
            ORDER BY e.expense_date DESC
            """;
        return jdbcTemplate.queryForList(sql, "%" + person + "%",
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public List<Map<String, Object>> getTopExpenses(int limit) {
        log.info("Fetching top {} expenses", limit);
        String sql = BASE_SQL + " ORDER BY e.amount DESC LIMIT ?";
        return jdbcTemplate.queryForList(sql, limit);
    }

    public BigDecimal calculateMonthlyAverage(int months) {
        log.info("Calculating monthly average for last {} months", months);
        LocalDate to = LocalDate.now();
        LocalDate from = to.minusMonths(months).withDayOfMonth(1);

        BigDecimal total = calculateTotal(from, to);
        if (months <= 0) return BigDecimal.ZERO;

        return total.divide(new BigDecimal(months), 2, RoundingMode.HALF_UP);
    }

    public String calculateTrend(String category, LocalDate from, LocalDate to) {
        log.info("Calculating trend for category: {} from {} to {}", category, from, to);

        String sql = """
            SELECT SUM(e.amount) as total
            FROM expenses e
            LEFT JOIN categories c ON e.category_id = c.id
            WHERE c.name ILIKE ?
            AND e.expense_date >= ? AND e.expense_date <= ?
            """;

        BigDecimal currentTotal = jdbcTemplate.queryForObject(sql, BigDecimal.class,
                "%" + category + "%", from.atStartOfDay(), to.plusDays(1).atStartOfDay());
        if (currentTotal == null) currentTotal = BigDecimal.ZERO;

        long days = java.time.temporal.ChronoUnit.DAYS.between(from, to) + 1;
        LocalDate prevTo = from.minusDays(1);
        LocalDate prevFrom = prevTo.minusDays(days - 1);

        BigDecimal prevTotal = jdbcTemplate.queryForObject(sql, BigDecimal.class,
                "%" + category + "%", prevFrom.atStartOfDay(), prevTo.plusDays(1).atStartOfDay());
        if (prevTotal == null) prevTotal = BigDecimal.ZERO;

        if (prevTotal.compareTo(BigDecimal.ZERO) == 0) {
            return String.format("Spending on %s is %s RON. No data for the previous period to compare.", category, currentTotal);
        }

        BigDecimal difference = currentTotal.subtract(prevTotal);
        BigDecimal percentage = difference.multiply(new BigDecimal("100")).divide(prevTotal, 2, RoundingMode.HALF_UP);

        String direction = difference.compareTo(BigDecimal.ZERO) >= 0 ? "increased" : "decreased";
        return String.format("Spending on %s has %s by %s%% (%s RON) compared to the previous period (Current: %s RON, Previous: %s RON).",
                category, direction, percentage.abs(), difference.abs(), currentTotal, prevTotal);
    }

    public List<Map<String, Object>> findByCategory(String category, LocalDate from, LocalDate to) {
        log.info("Finding expenses for category: {} from {} to {}", category, from, to);
        String sql = BASE_SQL + """
            WHERE c.name ILIKE ?
            AND e.expense_date >= ? AND e.expense_date <= ?
            ORDER BY e.expense_date DESC
            """;
        return jdbcTemplate.queryForList(sql, "%" + category + "%",
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public List<Map<String, Object>> findByLocation(String location, LocalDate from, LocalDate to) {
        log.info("Finding expenses for location: {} from {} to {}", location, from, to);
        String sql = BASE_SQL + """
            WHERE l.store ILIKE ?
            AND e.expense_date >= ? AND e.expense_date <= ?
            ORDER BY e.expense_date DESC
            """;
        return jdbcTemplate.queryForList(sql, "%" + location + "%",
                from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }

    public List<Map<String, Object>> findByAmount(BigDecimal amount) {
        log.info("Finding expenses with amount: {}", amount);
        String sql = BASE_SQL + " WHERE e.amount = ? ORDER BY e.expense_date DESC";
        return jdbcTemplate.query(sql, (rs, rowNum) -> {
            Map<String, Object> row = new java.util.HashMap<>();
            row.put("id", rs.getLong("id"));
            row.put("amount", rs.getBigDecimal("amount"));
            row.put("description", rs.getString("description"));
            row.put("date", rs.getTimestamp("date"));
            row.put(KEY_CATEGORY, rs.getString("category"));
            row.put("location", rs.getString("location"));
            row.put(KEY_PERSON, rs.getString("person"));
            row.put("currency", rs.getString("currency"));
            row.put("source_type", rs.getString("source_type"));
            return row;
        }, amount);
    }
}
