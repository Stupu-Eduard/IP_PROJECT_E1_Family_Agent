package com.proiect.controller;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;

import com.proiect.dto.response.AgentResponseDTO;
import com.proiect.dto.response.ChartResponseDTO;
import com.proiect.dto.response.TextResponseDTO;
import com.proiect.service.AgentChatService;
import com.proiect.service.ChartGenerationService;
import com.proiect.service.ExtractionService;
import com.proiect.service.RagRetrievalService;
import com.proiect.service.VisualIntentExtractor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.http.*;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@ActiveProfiles("test")
@Import(com.proiect.config.TestSecurityConfig.class)
class AgentControllerIntegrationTest {

    @MockBean
    private UserSessionRepository userSessionRepository;

    @Autowired
    private TestRestTemplate restTemplate;

    @MockBean
    private VisualIntentExtractor visualIntentExtractor;

    @MockBean
    private ChartGenerationService chartGenerationService;

    @MockBean
    private RagRetrievalService ragRetrievalService;

    @MockBean(name = "claudeModel")
    private dev.langchain4j.model.chat.ChatLanguageModel claudeModel;

    @MockBean(name = "deepseekModel")
    private dev.langchain4j.model.chat.ChatLanguageModel deepseekModel;

    @MockBean(name = "whisperModel")
    private dev.langchain4j.model.chat.ChatLanguageModel whisperModel;

    @MockBean
    private dev.langchain4j.model.embedding.EmbeddingModel embeddingModel;

    @MockBean
    private dev.langchain4j.store.embedding.qdrant.QdrantEmbeddingStore qdrantEmbeddingStore;

    @MockBean
    private dev.langchain4j.model.scoring.ScoringModel scoringModel;

    @MockBean
    private ExtractionService extractionService;

    @Test
    void testTextResponseEndToEnd() {
        when(visualIntentExtractor.extract("Cât am cheltuit?"))
                .thenReturn(com.proiect.model.ChartQueryIntent.builder()
                        .responseType("text")
                        .build());
        when(ragRetrievalService.askWithContext("Cât am cheltuit?"))
                .thenReturn("Ai cheltuit 100 RON.");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>("Cât am cheltuit?", headers);

        ResponseEntity<TextResponseDTO> response = restTemplate.exchange(
                "/v1/agent/chat", HttpMethod.POST, request, TextResponseDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("text", response.getBody().getType());
        assertEquals("Ai cheltuit 100 RON.", response.getBody().getMessage());
    }

    @Test
    void testChartResponseEndToEnd() {
        var payload = com.proiect.dto.response.ChartPayload.builder()
                .chartType("bar")
                .title("Comparație")
                .data(List.of(
                        Map.of("name", "Teodor", "total", 1200),
                        Map.of("name", "Maria", "total", 890)
                ))
                .dataKeys(List.of("total"))
                .xAxisKey("name")
                .build();

        when(visualIntentExtractor.extract("Compară cheltuielile"))
                .thenReturn(com.proiect.model.ChartQueryIntent.builder()
                        .responseType("chart")
                        .chartType("bar")
                        .build());
        when(chartGenerationService.generate(eq("Compară cheltuielile"), any()))
                .thenReturn(new ChartResponseDTO("Iată comparativul:", payload));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.TEXT_PLAIN);
        HttpEntity<String> request = new HttpEntity<>("Compară cheltuielile", headers);

        ResponseEntity<ChartResponseDTO> response = restTemplate.exchange(
                "/v1/agent/chat", HttpMethod.POST, request, ChartResponseDTO.class);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("chart", response.getBody().getType());
        assertEquals("Iată comparativul:", response.getBody().getMessage());
        assertNotNull(response.getBody().getPayload());
        assertEquals("bar", response.getBody().getPayload().getChartType());
        assertEquals(2, response.getBody().getPayload().getData().size());
    }
}
