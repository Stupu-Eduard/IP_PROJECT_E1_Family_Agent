package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatService {

    private final VisualIntentExtractor visualIntentExtractor;
    private final ChartGenerationService chartGenerationService;
    private final RagRetrievalService ragRetrievalService;
    private final FamilyMemberRepository familyMemberRepository;

    public AgentResponseDTO processQuery(String userMessage) {
        String userContext = buildUserContext();
        try {
            ChartQueryIntent intent = visualIntentExtractor.extract(userMessage);
            log.info("Extracted intent: type={}, chartType={}, groupBy={}",
                    intent.getResponseType(), intent.getChartType(), intent.getGroupBy());

            if ("chart".equalsIgnoreCase(intent.getResponseType())) {
                return chartGenerationService.generate(intent);
            }

            String query = userContext + userMessage;
            log.info("Query for RAG: {}", query);

            String textAnswer = ragRetrievalService.askWithContext(query);
            return toTextResponse(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}', falling back to text RAG: {}",
                    userMessage, e.getMessage());
            String query = userContext + userMessage;
            String textAnswer = ragRetrievalService.askWithContext(query);
            return toTextResponse(textAnswer);
        }
    }

    private String buildUserContext() {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return "";
        }
        String familyName = familyMemberRepository.findByUserId(user.getId())
                .stream()
                .findFirst()
                .map(fm -> fm.getFamily() != null ? fm.getFamily().getName() : null)
                .orElse(null);
        StringBuilder ctx = new StringBuilder("[IDENTITATE_AUTENTIFICATA: nume='")
                .append(user.getName())
                .append("', user_id=").append(user.getId());
        if (familyName != null) {
            ctx.append(", familia='").append(familyName).append("'");
        }
        ctx.append("] ");
        return ctx.toString();
    }

    private TextResponseDTO toTextResponse(String textAnswer) {
        if (textAnswer == null || textAnswer.isBlank()) {
            log.error("RAG pipeline returned null or blank text answer");
            return new TextResponseDTO(
                    "Nu am putut genera un răspuns. Încearcă din nou sau reformulează întrebarea.");
        }
        String cleaned = stripMarkdown(textAnswer);
        return new TextResponseDTO(cleaned);
    }

    /**
     * Light markdown sanitizer: removes only code fences to prevent UI breakage,
     * but preserves all other formatting (lists, bold, headers, tables, paragraphs).
     */
    static String stripMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = text;
        // Remove markdown code fences (```json, ```java, etc.)
        cleaned = cleaned.replaceAll("```[a-zA-Z]*\\s*", "");
        cleaned = cleaned.replace("```", "");
        return cleaned.trim();
    }
}
