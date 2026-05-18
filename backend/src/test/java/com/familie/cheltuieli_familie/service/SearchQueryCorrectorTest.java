package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchQueryCorrectorTest {

    @Mock
    private JdbcTemplate jdbcTemplate;

    @InjectMocks
    private SearchQueryCorrector corrector;

    @Test
    void shouldCorrectTypoInQuery() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.emptyList());

        String corrected = corrector.correctQuery("mancarre");
        assertEquals("mancare", corrected);
    }

    @Test
    void shouldCorrectMultipleTypos() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.emptyList());

        String corrected = corrector.correctQuery("mancarre benzina");
        assertEquals("mancare benzina", corrected);
    }

    @Test
    void shouldLeaveCorrectWordsUnchanged() {
        when(jdbcTemplate.queryForList(anyString(), eq(String.class)))
                .thenReturn(Collections.emptyList());

        String corrected = corrector.correctQuery("mancare");
        assertEquals("mancare", corrected);
    }

    @Test
    void shouldUseDictionaryFromDatabase() {
        when(jdbcTemplate.queryForList("SELECT DISTINCT name FROM categories WHERE name IS NOT NULL", String.class))
                .thenReturn(List.of("Mancare", "Transport"));
        when(jdbcTemplate.queryForList("SELECT DISTINCT store FROM locations WHERE store IS NOT NULL", String.class))
                .thenReturn(Collections.emptyList());
        when(jdbcTemplate.queryForList("SELECT DISTINCT name FROM users WHERE name IS NOT NULL", String.class))
                .thenReturn(Collections.emptyList());

        String corrected = corrector.correctQuery("mancarre");
        assertEquals("mancare", corrected);
    }

    @Test
    void shouldHandleNullQuery() {
        assertNull(corrector.correctQuery(null));
    }

    @Test
    void shouldHandleBlankQuery() {
        assertEquals("", corrector.correctQuery(""));
    }
}
