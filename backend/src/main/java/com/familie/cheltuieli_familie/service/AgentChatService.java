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
     * Aggressive markdown sanitizer: strips all markdown formatting to produce plain text.
     * Removes bold, italics, headers, tables, blockquotes, lists, code blocks, smart quotes, etc.
     */
    static String stripMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = text;

        // Remove markdown code blocks with optional language tag (multiline)
        cleaned = cleaned.replaceAll("(?s)```[a-zA-Z]*\\r?\\n.*?```", "");
        // Remove remaining code fences
        cleaned = cleaned.replaceAll("```[a-zA-Z]*\\s*", "");
        cleaned = cleaned.replace("```", "");

        // Remove inline code backticks
        cleaned = cleaned.replaceAll("`([^`]*)`", "$1");

        // Remove bold **text** and __text__
        cleaned = cleaned.replaceAll("\\*\\*([^*]*)\\*\\*", "$1");
        cleaned = cleaned.replaceAll("__([^_]*)__", "$1");

        // Remove italic *text* and _text_
        cleaned = cleaned.replaceAll("(?<!\\*)\\*(?!\\*)([^*]*)\\*", "$1");
        cleaned = cleaned.replaceAll("(?<!_)_(?!_)([^_]*)_", "$1");

        // Remove strikethrough ~~text~~
        cleaned = cleaned.replaceAll("~~([^~]*)~~", "$1");

        // Remove headers (# ## ### etc.)
        cleaned = cleaned.replaceAll("(?m)^#{1,6}\\s*", "");

        // Remove blockquote markers at line start
        cleaned = cleaned.replaceAll("(?m)^>\\s*", "");

        // Remove markdown table separators like |---|---|
        cleaned = java.util.Arrays.stream(cleaned.split("\n", -1))
                .filter(line -> !isTableSeparatorLine(line))
                .collect(java.util.stream.Collectors.joining("\n"));
        // Remove table cell pipes, keep inner text
        cleaned = cleaned.replace("|", " ");

        // Remove list markers at line start (*, -, + followed by space)
        cleaned = cleaned.replaceAll("(?m)^\\s*[*+\\-]\\s+", "");
        // Remove numbered list markers (1. 2. etc.) at line start
        cleaned = cleaned.replaceAll("(?m)^\\s*\\d+\\.\\s+", "");

        // Normalize smart quotes to standard quotes
        cleaned = cleaned.replace("\"", "\"");
        cleaned = cleaned.replace("\"", "\"");
        cleaned = cleaned.replace("'", "'");
        cleaned = cleaned.replace("'", "'");

        // Remove horizontal rules
        cleaned = cleaned.replaceAll("(?m)^\\s*[-_*]{3,}\\s*$", "");

        // Collapse multiple consecutive newlines to max 2
        cleaned = cleaned.replaceAll("\\n{3,}", "\\n\\n");

        // Trim each line and the whole text
        cleaned = java.util.Arrays.stream(cleaned.split("\n", -1))
                .map(String::strip)
                .collect(java.util.stream.Collectors.joining("\n"));

        return cleaned.trim();
    }

    private static boolean isTableSeparatorLine(String line) {
        String stripped = line.strip();
        if (stripped.isEmpty()) return false;
        String inner = stripped.startsWith("|") ? stripped.substring(1) : stripped;
        if (inner.endsWith("|")) inner = inner.substring(0, inner.length() - 1);
        if (!inner.contains("-")) return false;
        for (char c : inner.toCharArray()) {
            if (c != '-' && c != ':' && c != '|' && c != ' ' && c != '\t') return false;
        }
        return true;
    }
}
