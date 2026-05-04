package com.familie.cheltuieli_familie.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.context.ActiveProfiles;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class DateNormalizerTest {

    @Test
    void testResolveRelativeDate_yesterday() {
        LocalDate result = DateNormalizer.resolveRelativeDate("Am platit ieri la magazin");
        assertEquals(LocalDate.now().minusDays(1), result);
    }

    @Test
    void testResolveRelativeDate_dayBeforeYesterday() {
        LocalDate result = DateNormalizer.resolveRelativeDate("alaltăieri am cumparat paine");
        assertEquals(LocalDate.now().minusDays(2), result);
    }

    @Test
    void testResolveRelativeDate_today() {
        LocalDate result = DateNormalizer.resolveRelativeDate("astăzi plătesc factura");
        assertEquals(LocalDate.now(), result);
    }

    @Test
    void testResolveRelativeDate_dayAfterTomorrow() {
        LocalDate result = DateNormalizer.resolveRelativeDate("poimâine merg la dentist");
        assertEquals(LocalDate.now().plusDays(2), result);
    }

    @Test
    void testResolveRelativeDate_lastWeek() {
        LocalDate result = DateNormalizer.resolveRelativeDate("săptămâna trecută am cheltuit mult");
        assertEquals(LocalDate.now().minusWeeks(1), result);
    }

    @Test
    void testResolveRelativeDate_lastMonth() {
        LocalDate result = DateNormalizer.resolveRelativeDate("luna trecută am platit chiria");
        assertEquals(LocalDate.now().minusMonths(1), result);
    }

    @Test
    void testResolveRelativeDate_namedMonth() {
        int currentYear = LocalDate.now().getYear();
        assertEquals(LocalDate.of(currentYear, 1, 15), DateNormalizer.resolveRelativeDate("15 ianuarie"));
        assertEquals(LocalDate.of(currentYear, 3, 8), DateNormalizer.resolveRelativeDate("8 martie"));
        assertEquals(LocalDate.of(currentYear, 12, 25), DateNormalizer.resolveRelativeDate("25 decembrie"));
    }

    @Test
    void testResolveRelativeDate_numericFormats() {
        assertEquals(LocalDate.of(2023, 10, 20), DateNormalizer.resolveRelativeDate("20.10.2023"));
        assertEquals(LocalDate.of(2024, 5, 15), DateNormalizer.resolveRelativeDate("15-05-2024"));
        assertEquals(LocalDate.of(2025, 7, 30), DateNormalizer.resolveRelativeDate("30/07/25"));
    }

    @Test
    void testResolveRelativeDate_nullAndEmpty() {
        assertEquals(LocalDate.now(), DateNormalizer.resolveRelativeDate(null));
        assertEquals(LocalDate.now(), DateNormalizer.resolveRelativeDate(""));
    }
}
