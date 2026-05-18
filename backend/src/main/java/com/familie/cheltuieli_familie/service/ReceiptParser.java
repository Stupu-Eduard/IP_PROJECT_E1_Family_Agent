package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import dev.langchain4j.model.chat.ChatLanguageModel;
import dev.langchain4j.service.AiServices;
import dev.langchain4j.service.SystemMessage;
import dev.langchain4j.service.UserMessage;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.List;

@Service
@Slf4j
public class ReceiptParser {

    private final ChatLanguageModel chatLanguageModel;
    private final ObjectMapper objectMapper;

    private static final DateTimeFormatter[] DATE_FORMATTERS = {
            DateTimeFormatter.ofPattern("dd/MM/yyyy"),
            DateTimeFormatter.ofPattern("dd.MM.yyyy"),
            DateTimeFormatter.ofPattern("yyyy-MM-dd"),
            DateTimeFormatter.ofPattern("dd-MM-yyyy"),
    };

    public ReceiptParser(ChatLanguageModel chatLanguageModel) {
        this.chatLanguageModel = chatLanguageModel;
        this.objectMapper = new ObjectMapper();
    }

    public ParsedReceipt parseReceipt(String ocrText) {
        if (ocrText == null || ocrText.isBlank()) {
            log.warn("OCR text is empty, cannot parse receipt");
            return null;
        }

        try {
            ReceiptExtractor extractor = AiServices.builder(ReceiptExtractor.class)
                    .chatLanguageModel(chatLanguageModel)
                    .build();

            String json = extractor.extract(ocrText);
            log.info("LLM receipt extraction raw JSON: {}", json);

            // Clean markdown code blocks if present
            json = stripMarkdownFences(json);

            ParsedReceipt receipt = objectMapper.readValue(json, ParsedReceipt.class);

            // Validate and normalize
            if (receipt.getTotalAmount() == null || receipt.getTotalAmount().compareTo(BigDecimal.ZERO) <= 0) {
                log.warn("LLM extraction returned invalid amount: {}", receipt.getTotalAmount());
                return null;
            }

            receipt.setDate(normalizeDate(receipt.getDate()));
            receipt.setStoreName(normalizeText(receipt.getStoreName()));
            receipt.setCategory(normalizeText(receipt.getCategory()));

            log.info("Parsed receipt: store={}, amount={}, date={}, category={}",
                    receipt.getStoreName(), receipt.getTotalAmount(), receipt.getDate(), receipt.getCategory());

            return receipt;

        } catch (Exception e) {
            log.error("Failed to parse receipt with LLM: {}", e.getMessage(), e);
            return null;
        }
    }

    private String normalizeDate(String dateStr) {
        if (dateStr == null || dateStr.isBlank()) {
            return null;
        }
        for (DateTimeFormatter formatter : DATE_FORMATTERS) {
            try {
                LocalDate date = LocalDate.parse(dateStr, formatter);
                return date.toString();
            } catch (DateTimeParseException ignored) {
                // Date format did not match; try next formatter
            }
        }
        return null;
    }

    private String normalizeText(String text) {
        if (text == null) return null;
        return text.trim();
    }

    private String stripMarkdownFences(String raw) {
        if (raw == null) return null;
        String trimmed = raw.trim();
        if (trimmed.startsWith("```")) {
            trimmed = trimmed.replaceFirst("```json\\s*", "").replaceFirst("```\\s*", "");
            int lastFence = trimmed.lastIndexOf("```");
            if (lastFence >= 0) trimmed = trimmed.substring(0, lastFence).trim();
        }
        return trimmed;
    }

    public interface ReceiptExtractor {
        @SystemMessage("""
            Ești un parser inteligent de bonuri fiscale și extrase bancare. Extrage informațiile structurate din textul OCR furnizat.

            Reguli:
            1. Identifică numele magazinului/băncii (storeName) - de obicei în partea de sus a bonului.
            2. Identifică suma totală (totalAmount) - caută "TOTAL", "SUMĂ", "TOTAL DE PLATĂ".
            3. Identifică data (date) - în format dd/MM/yyyy sau similar.
            4. Identifică categoria (category) - inferă din tipul magazinului sau articolelor:
               - Supermarketuri (Lidl, Kaufland, Mega Image, Carrefour) → "Mâncare"
               - Benzinării (OMV, Petrom, Rompetrol, Shell) → "Transport"
               - Farmacii (Catena, Sensiblu, Dona) → "Sănătate"
               - Restaurante, cafenele → "Divertisment"
               - Magazine de haine (H&M, Zara, etc.) → "Haine"
               - Facturi utilități → "Utilități"
               - Dacă nu ești sigur, folosește "Diverse"
            5. Lista de articole (items) este opțională.

            Răspunde EXCLUSIV cu un JSON valid, fără markdown, fără explicații suplimentare.
            Format:
            {
              "storeName": "string",
              "totalAmount": "number",
              "date": "string (dd/MM/yyyy)",
              "category": "string",
              "items": [
                {"name": "string", "quantity": "number", "unitPrice": "number"}
              ]
            }
            """)
        String extract(@UserMessage String ocrText);
    }

    public static class ParsedReceipt {
        private String storeName;
        private BigDecimal totalAmount;
        private String date;
        private String category;
        private List<ReceiptItem> items;

        public String getStoreName() { return storeName; }
        public void setStoreName(String storeName) { this.storeName = storeName; }
        public BigDecimal getTotalAmount() { return totalAmount; }
        public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        public String getCategory() { return category; }
        public void setCategory(String category) { this.category = category; }
        public List<ReceiptItem> getItems() { return items; }
        public void setItems(List<ReceiptItem> items) { this.items = items; }
    }

    public static class ReceiptItem {
        private String name;
        private BigDecimal quantity;
        private BigDecimal unitPrice;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public BigDecimal getQuantity() { return quantity; }
        public void setQuantity(BigDecimal quantity) { this.quantity = quantity; }
        public BigDecimal getUnitPrice() { return unitPrice; }
        public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    }
}
