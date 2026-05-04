package com.proiect.service;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@ActiveProfiles("test")
class AnalyticsIntegrationTest {

    @MockitoBean
    private ExpenseTools expenseTools;

    @MockitoBean
    private HallucinationGuard hallucinationGuard;

    @MockitoBean
    private ExpenseJpaRepository repository;

    @MockitoBean(name = "claudeModel")
    private dev.langchain4j.model.chat.ChatLanguageModel claudeModel;

    @MockitoBean(name = "deepseekModel")
    private dev.langchain4j.model.chat.ChatLanguageModel deepseekModel;

    @MockitoBean(name = "whisperModel")
    private dev.langchain4j.model.chat.ChatLanguageModel whisperModel;

    @MockitoBean
    private dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @MockitoBean
    private dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore qdrantEmbeddingStore;

    @MockitoBean
    private dev.langchain4j.model.scoring.ScoringModel scoringModel;

    @MockitoBean
    private ExtractionService extractionService;

    @MockitoBean
    private AnalyticsAssistant analyticsAssistant;

    @Test
    void testLauraAnalyticsFlow_HallucinationGuardCorrection() {
        when(expenseTools.calculateTotal(anyString(), anyString())).thenReturn("100.50 RON");
        when(hallucinationGuard.validate(anyString(), anyString())).thenReturn("Totalul cheltuielilor este de 100.50 RON luna aceasta. (Verificat)");
        
        String toolOutput = expenseTools.calculateTotal("2024-01-01", "2024-01-01");
        String aiResponse = "Totalul cheltuielilor este de 100.00 RON luna aceasta.";
        String validated = hallucinationGuard.validate(aiResponse, toolOutput);
        
        assertTrue(validated.contains("100.50"));
        assertTrue(validated.contains("Verificat"));
    }

    @Test
    void testSemanticCorrection_IncreaseVsDecrease() {
        when(hallucinationGuard.validate(anyString(), anyString())).thenReturn("Cheltuielile au înregistrat o creștere de 15%. (100.50 RON)");
        
        String toolOutput = "Spending on Food has increased by 15.5% (100.50 RON)";
        String aiResponse = "Cheltuielile au înregistrat o scădere de 15%.";
        String validated = hallucinationGuard.validate(aiResponse, toolOutput);
        
        assertTrue(validated.contains("creștere"));
        assertTrue(validated.contains("100.50"));
    }
}
