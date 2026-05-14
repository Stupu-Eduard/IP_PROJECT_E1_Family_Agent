package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;

import java.math.BigDecimal;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@ActiveProfiles("test")
@TestPropertySource(properties = {
    "DEEPSEEK_API_KEY=test-key",
    "OPENROUTER_API_KEY=test-key"
})
class AnalyticsIntegrationTest {

    @Autowired
    private ExpenseTools expenseTools;

    @Autowired
    private HallucinationGuard hallucinationGuard;

    @MockBean
    private ExpenseJpaRepository repository;

    @MockBean
    private ExpenseAnalyticsService analyticsService;

    @Test
    void testLauraAnalyticsFlow_HallucinationGuardCorrection() {
        // GIVEN
        when(analyticsService.calculateTotal(LocalDate.of(2024, 1, 1), LocalDate.of(2024, 1, 1)))
                .thenReturn(new BigDecimal("100.50"));

        // WHEN - Tool-ul scoate 100.50 RON
        String toolOutput = expenseTools.calculateTotal("2024-01-01", "2024-01-01");
        
        // Simulăm un AI care halucinează (rotunjire proastă: 100 RON)
        String aiResponse = "Totalul cheltuielilor este de 100.00 RON luna aceasta.";
        
        // VALIDATE
        String validated = hallucinationGuard.validate(aiResponse, toolOutput);
        
        // THEN
        assertTrue(validated.contains("100.50"), "Guard ar fi trebuit să corecteze cifra la 100.50");
        assertFalse(validated.contains("100.00"), "Valoarea inițială 100.00 ar fi trebuit înlocuită");
    }

    @Test
    void testSemanticCorrection_IncreaseVsDecrease() {
        // Simulăm un tool care indică o creștere (increased)
        String toolOutput = "Spending on Food has increased by 15.5% (100.50 RON)";
        
        // AI-ul spune greșit că a scăzut
        String aiResponse = "Cheltuielile au înregistrat o scădere de 15%.";
        
        String validated = hallucinationGuard.validate(aiResponse, toolOutput);
        
        assertTrue(validated.contains("creștere"), "Guard ar fi trebuit să schimbe 'scădere' în 'creștere'");
        assertFalse(validated.contains("scădere"), "Cuvântul 'scădere' ar fi trebuit înlocuit");
    }
}
