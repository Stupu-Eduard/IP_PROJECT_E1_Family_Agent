package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class ExpenseAnalyticsServiceTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private ExpenseAnalyticsService analyticsService;

    private List<Map<String, Object>> sampleExpenses;

    @BeforeEach
    void setUp() {
        sampleExpenses = List.of(
                Map.of("amount", new BigDecimal("100.00"), "category", "Food", "person", "Alice", "date", LocalDate.now()),
                Map.of("amount", new BigDecimal("200.00"), "category", "Transport", "person", "Bob", "date", LocalDate.now()),
                Map.of("amount", new BigDecimal("150.00"), "category", "Food", "person", "Alice", "date", LocalDate.now())
        );
    }

    @Test
    void testCalculateTotal() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("450.00"));

        BigDecimal total = analyticsService.calculateTotal(from, to, null, 1L);

        assertEquals(new BigDecimal("450.00"), total);
    }

    @Test
    void testByCategory() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(
                        Map.of("category", "Food", "total", new BigDecimal("250.00")),
                        Map.of("category", "Transport", "total", new BigDecimal("200.00"))
                ));

        Map<String, BigDecimal> result = analyticsService.byCategory(from, to, null, 1L);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("250.00"), result.get("Food"));
        assertEquals(new BigDecimal("200.00"), result.get("Transport"));
    }

    @Test
    void testCompareMembers() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any(), any()))
                .thenReturn(List.of(
                        Map.of("person", "Alice", "total", new BigDecimal("250.00")),
                        Map.of("person", "Bob", "total", new BigDecimal("200.00"))
                ));

        Map<String, BigDecimal> result = analyticsService.compareMembers(from, to, null, 1L);

        assertEquals(2, result.size());
        assertEquals(new BigDecimal("250.00"), result.get("Alice"));
        assertEquals(new BigDecimal("200.00"), result.get("Bob"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testDetectAnomalies() {
        when(jdbcTemplate.queryForList(anyString(), any(BigDecimal.class), any(Long.class)))
                .thenReturn((List) sampleExpenses);

        List<Map<String, Object>> anomalies = analyticsService.detectAnomalies(new BigDecimal("180.00"), null, 1L);

        assertEquals(3, anomalies.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByPerson() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForList(anyString(), anyString(), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class), any(Long.class)))
                .thenReturn((List) List.of(
                        Map.of("amount", new BigDecimal("100.00"), "category", "Food", "person", "Alice"),
                        Map.of("amount", new BigDecimal("150.00"), "category", "Food", "person", "Alice")
                ));

        List<Map<String, Object>> result = analyticsService.findByPerson("Alice", from, to, null, 1L);

        assertEquals(2, result.size());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByPersonNoMatch() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForList(anyString(), anyString(), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class), any(Long.class)))
                .thenReturn((List) List.of());

        List<Map<String, Object>> result = analyticsService.findByPerson("Charlie", from, to, null, 1L);

        assertTrue(result.isEmpty());
    }

    @SuppressWarnings("unchecked")
    @Test
    void testGetTopExpenses() {
        when(jdbcTemplate.queryForList(anyString(), any(Long.class), any(Integer.class)))
                .thenReturn((List) sampleExpenses);

        List<Map<String, Object>> result = analyticsService.getTopExpenses(3, null, 1L);

        assertEquals(3, result.size());
    }

    @Test
    void testCalculateMonthlyAverage() {
        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), any(), any(), any()))
                .thenReturn(new BigDecimal("450.00"));

        BigDecimal avg = analyticsService.calculateMonthlyAverage(3, null, 1L);

        assertEquals(new BigDecimal("150.00"), avg);
    }

    @Test
    void testCalculateMonthlyAverageZeroMonths() {
        BigDecimal avg = analyticsService.calculateMonthlyAverage(0, null, 1L);
        assertEquals(BigDecimal.ZERO, avg);
    }

    @Test
    void testCalculateTrendIncrease() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), anyString(), any(), any(), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(new BigDecimal("100.00"));

        String trend = analyticsService.calculateTrend("Food", from, to, null, 1L);

        assertTrue(trend.contains("increased"));
        assertTrue(trend.contains("100.00"));
    }

    @Test
    void testCalculateTrendDecrease() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), anyString(), any(), any(), any()))
                .thenReturn(new BigDecimal("50.00"))
                .thenReturn(new BigDecimal("100.00"));

        String trend = analyticsService.calculateTrend("Food", from, to, null, 1L);

        assertTrue(trend.contains("decreased"));
        assertTrue(trend.contains("50.00"));
    }

    @Test
    void testCalculateTrendNoPreviousData() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), anyString(), any(), any(), any()))
                .thenReturn(new BigDecimal("200.00"))
                .thenReturn(BigDecimal.ZERO);

        String trend = analyticsService.calculateTrend("Food", from, to, null, 1L);

        assertTrue(trend.contains("No data for the previous period"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindExpenses() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForList(anyString(), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class), any(Long.class)))
                .thenReturn((List) sampleExpenses);

        List<Map<String, Object>> result = analyticsService.findExpenses(from, to, null, 1L);

        assertEquals(3, result.size());
        assertEquals(new BigDecimal("100.00"), result.get(0).get("amount"));
        assertEquals("Food", result.get(0).get("category"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByCategory() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForList(anyString(), anyString(), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class), any(Long.class)))
                .thenReturn((List) List.of(
                        Map.of("amount", new BigDecimal("100.00"), "category", "Food", "person", "Alice")
                ));

        List<Map<String, Object>> result = analyticsService.findByCategory("Food", from, to, null, 1L);

        assertEquals(1, result.size());
        assertEquals("Food", result.get(0).get("category"));
    }

    @SuppressWarnings("unchecked")
    @Test
    void testFindByLocation() {
        LocalDate from = LocalDate.now().minusDays(1);
        LocalDate to = LocalDate.now();
        when(jdbcTemplate.queryForList(anyString(), anyString(), any(java.time.LocalDateTime.class), any(java.time.LocalDateTime.class), any(Long.class)))
                .thenReturn((List) List.of(
                        Map.of("amount", new BigDecimal("50.00"), "location", "StoreA", "person", "Bob")
                ));

        List<Map<String, Object>> result = analyticsService.findByLocation("StoreA", from, to, null, 1L);

        assertEquals(1, result.size());
        assertEquals("StoreA", result.get(0).get("location"));
    }

    @Test
    void testCalculateTrendNullCurrentTotal() {
        LocalDate from = LocalDate.of(2024, 3, 1);
        LocalDate to = LocalDate.of(2024, 3, 31);

        when(jdbcTemplate.queryForObject(anyString(), eq(BigDecimal.class), anyString(), any(), any(), any()))
                .thenReturn(null)
                .thenReturn(new BigDecimal("100.00"));

        String trend = analyticsService.calculateTrend("Food", from, to, null, 1L);

        assertTrue(trend.contains("decreased"));
        assertTrue(trend.contains("0 RON"));
        assertTrue(trend.contains("100.00 RON"));
    }

    @Test
    void testFindByAmount() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenReturn(List.of(
                        Map.of("id", 1L, "amount", new BigDecimal("99.99"), "description", "Test", "date", LocalDate.now(),
                                "category", "Food", "location", "Kaufland", "person", "Alice", "currency", "RON", "source_type", "MANUAL")
                ));

        List<Map<String, Object>> result = analyticsService.findByAmount(new BigDecimal("99.99"), null, 1L);

        assertEquals(1, result.size());
        assertEquals(new BigDecimal("99.99"), result.get(0).get("amount"));
        assertEquals("Food", result.get(0).get("category"));
    }

    @Test
    void testFindByAmountUsesKeyConstants() {
        when(jdbcTemplate.query(anyString(), any(org.springframework.jdbc.core.RowMapper.class), any(), any()))
                .thenAnswer(invocation -> {
                    org.springframework.jdbc.core.RowMapper<Map<String, Object>> rowMapper = invocation.getArgument(1);
                    java.sql.ResultSet rs = org.mockito.Mockito.mock(java.sql.ResultSet.class);
                    when(rs.getLong("id")).thenReturn(1L);
                    when(rs.getBigDecimal("amount")).thenReturn(new BigDecimal("99.99"));
                    when(rs.getString("description")).thenReturn("Test");
                    when(rs.getTimestamp("date")).thenReturn(new java.sql.Timestamp(System.currentTimeMillis()));
                    when(rs.getString("category")).thenReturn("Food");
                    when(rs.getString("location")).thenReturn("Kaufland");
                    when(rs.getString("person")).thenReturn("Alice");
                    when(rs.getString("currency")).thenReturn("RON");
                    when(rs.getString("source_type")).thenReturn("MANUAL");
                    try {
                        return List.of(rowMapper.mapRow(rs, 0));
                    } catch (Exception e) {
                        throw new RuntimeException(e);
                    }
                });

        List<Map<String, Object>> result = analyticsService.findByAmount(new BigDecimal("99.99"), null, 1L);

        assertEquals(1, result.size());
        Map<String, Object> row = result.get(0);
        assertEquals("Food", row.get("category"));
        assertEquals("Alice", row.get("person"));
    }
}
