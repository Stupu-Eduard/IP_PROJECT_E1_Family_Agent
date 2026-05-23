package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.response.AgentResponseDTO;
import com.familie.cheltuieli_familie.dto.response.TextResponseDTO;
import com.familie.cheltuieli_familie.model.ChartQueryIntent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AgentChatService {

    private static final int MAX_MESSAGE_LENGTH = 2000;

    private final VisualIntentExtractor visualIntentExtractor;
    private final ChartGenerationService chartGenerationService;
    private final RagRetrievalService ragRetrievalService;

    public AgentResponseDTO processQuery(String userMessage) {
        if (userMessage == null || userMessage.isBlank()) {
            return new TextResponseDTO("Mesajul nu poate fi gol.");
        }
        if (userMessage.length() > MAX_MESSAGE_LENGTH) {
            return new TextResponseDTO(
                    "Mesajul este prea lung. Limita maximă este de " + MAX_MESSAGE_LENGTH + " caractere.");
        }
        String sanitized = sanitizeInput(userMessage);

        try {
            ChartQueryIntent intent = visualIntentExtractor.extract(sanitized);
            log.info("Extracted intent: type={}, chartType={}, groupBy={}",
                    intent.getResponseType(), intent.getChartType(), intent.getGroupBy());

            if ("chart".equalsIgnoreCase(intent.getResponseType())) {
                return chartGenerationService.generate(intent);
            }

            String textAnswer = ragRetrievalService.askWithContext(sanitized);
            return toTextResponse(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}': {}", sanitized, e.getMessage());
            return new TextResponseDTO(
                    "Nu am putut procesa cererea. Încearcă din nou sau reformulează întrebarea.");
        }
    }

    private String sanitizeInput(String input) {
        // Remove null bytes and control characters (except newlines and tabs)
        return input.replaceAll("[\\x00-\\x08\\x0B\\x0C\\x0E-\\x1F]", "").trim();
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
