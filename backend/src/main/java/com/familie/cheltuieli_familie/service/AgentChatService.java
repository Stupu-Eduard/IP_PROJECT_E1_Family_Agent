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

    private final VisualIntentExtractor visualIntentExtractor;
    private final ChartGenerationService chartGenerationService;
    private final RagRetrievalService ragRetrievalService;

    public AgentResponseDTO processQuery(String userMessage) {
        try {
            ChartQueryIntent intent = visualIntentExtractor.extract(userMessage);
            log.info("Extracted intent: type={}, chartType={}, groupBy={}",
                    intent.getResponseType(), intent.getChartType(), intent.getGroupBy());

            if ("chart".equalsIgnoreCase(intent.getResponseType())) {
                return chartGenerationService.generate(intent);
            }

            // Clean query text for better RAG embedding
            String cleanQuery = cleanQueryForRag(userMessage);
            log.info("Cleaned query for RAG: {}", cleanQuery);

            // Default to text response via existing RAG pipeline
            String textAnswer = ragRetrievalService.askWithContext(cleanQuery);
            return toTextResponse(textAnswer);

        } catch (Exception e) {
            log.warn("Chart pipeline failed for query '{}', falling back to text RAG: {}",
                    userMessage, e.getMessage());
            String cleanQuery = cleanQueryForRag(userMessage);
            String textAnswer = ragRetrievalService.askWithContext(cleanQuery);
            return toTextResponse(textAnswer);
        }
    }

    /**
     * Cleans user query by removing stop words and filler text for better semantic search.
     */
    private String cleanQueryForRag(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = text.trim();
        // Remove leading/trailing quotes
        while (!cleaned.isEmpty() && (cleaned.charAt(0) == '\'' || cleaned.charAt(0) == '"')) {
            cleaned = cleaned.substring(1);
        }
        while (!cleaned.isEmpty() && (cleaned.charAt(cleaned.length() - 1) == '\'' || cleaned.charAt(cleaned.length() - 1) == '"')) {
            cleaned = cleaned.substring(0, cleaned.length() - 1);
        }
        // Remove common Romanian stop words and filler phrases (case-insensitive, whole words only)
        String[] stopWords = {"salut", "buna", "te rog", "poti sa", "mi spui", "spune-mi", "ceva despre", "am adaugat", "o cheltuiala", "despre", "vreau sa stiu", "ai gasit"};
        String lower = cleaned.toLowerCase();
        for (String sw : stopWords) {
            // Replace whole-word matches only
            int idx;
            while ((idx = lower.indexOf(sw)) >= 0) {
                boolean startBoundary = idx == 0 || !Character.isLetterOrDigit(lower.charAt(idx - 1));
                boolean endBoundary = idx + sw.length() >= lower.length() || !Character.isLetterOrDigit(lower.charAt(idx + sw.length()));
                if (startBoundary && endBoundary) {
                    cleaned = cleaned.substring(0, idx) + " " + cleaned.substring(idx + sw.length());
                    lower = cleaned.toLowerCase();
                } else {
                    break;
                }
            }
        }
        // Collapse multiple spaces
        while (cleaned.contains("  ")) {
            cleaned = cleaned.replace("  ", " ");
        }
        return cleaned.trim();
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
     * Strips common markdown formatting from LLM responses to ensure clean text output.
     */
    static String stripMarkdown(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String cleaned = text;
        // Remove markdown tables: lines containing | ... |
        StringBuilder noTables = new StringBuilder();
        for (String line : cleaned.split("\n", -1)) {
            String t = line.trim();
            if (!t.startsWith("|") || t.indexOf('|', 1) < 0) {
                noTables.append(line).append("\n");
            }
        }
        cleaned = noTables.toString();
        // Remove bold/italic markers
        cleaned = cleaned.replace("**", "");
        cleaned = cleaned.replace("__", "");
        // Remove single * and _ that are word boundaries (simple approach)
        cleaned = boundaryReplace(cleaned, '*');
        cleaned = boundaryReplace(cleaned, '_');
        // Remove code block markers
        cleaned = cleaned.replace("```", "");
        // Remove inline code backticks and content between them
        cleaned = removeBetween(cleaned, '`', '`');
        // Remove headers (# ... at start of line)
        StringBuilder noHeaders = new StringBuilder();
        for (String line : cleaned.split("\n", -1)) {
            String trimmed = line.trim();
            int hashCount = 0;
            while (hashCount < trimmed.length() && hashCount < 6 && trimmed.charAt(hashCount) == '#') {
                hashCount++;
            }
            if (hashCount > 0 && hashCount < trimmed.length() && trimmed.charAt(hashCount) == ' ') {
                noHeaders.append(trimmed.substring(hashCount + 1)).append("\n");
            } else {
                noHeaders.append(line).append("\n");
            }
        }
        cleaned = noHeaders.toString();
        // Remove bullet points and numbered lists at line start
        StringBuilder noLists = new StringBuilder();
        for (String line : cleaned.split("\n", -1)) {
            String trimmed = line.trim();
            if (trimmed.startsWith("- ") || trimmed.startsWith("* ") || trimmed.startsWith("+ ")) {
                noLists.append(line.substring(line.indexOf(trimmed) + 2)).append("\n");
            } else if (isNumberedListItem(trimmed)) {
                int dotIdx = trimmed.indexOf('.');
                noLists.append(line.substring(line.indexOf(trimmed) + dotIdx + 1).trim()).append("\n");
            } else {
                noLists.append(line).append("\n");
            }
        }
        cleaned = noLists.toString();
        // Collapse multiple newlines
        while (cleaned.contains("\n\n")) {
            cleaned = cleaned.replace("\n\n", "\n");
        }
        return cleaned.trim();
    }

    private static String boundaryReplace(String text, char marker) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < text.length(); i++) {
            if (text.charAt(i) == marker) {
                boolean leftBoundary = i == 0 || !Character.isLetterOrDigit(text.charAt(i - 1));
                boolean rightBoundary = i + 1 >= text.length() || !Character.isLetterOrDigit(text.charAt(i + 1));
                if (!(leftBoundary && rightBoundary)) {
                    sb.append(marker);
                }
            } else {
                sb.append(text.charAt(i));
            }
        }
        return sb.toString();
    }

    private static String removeBetween(String text, char start, char end) {
        StringBuilder sb = new StringBuilder();
        int i = 0;
        while (i < text.length()) {
            int s = text.indexOf(start, i);
            if (s < 0) {
                sb.append(text, i, text.length());
                break;
            }
            int e = text.indexOf(end, s + 1);
            if (e < 0) {
                sb.append(text, i, text.length());
                break;
            }
            sb.append(text, i, s);
            i = e + 1;
        }
        return sb.toString();
    }

    private static boolean isNumberedListItem(String trimmed) {
        int dotIdx = trimmed.indexOf('.');
        if (dotIdx <= 0 || dotIdx >= trimmed.length() - 1 || trimmed.charAt(dotIdx + 1) != ' ') {
            return false;
        }
        for (int i = 0; i < dotIdx; i++) {
            if (!Character.isDigit(trimmed.charAt(i))) {
                return false;
            }
        }
        return true;
    }
}
