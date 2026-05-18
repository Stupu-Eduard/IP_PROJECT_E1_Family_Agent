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
import java.util.function.Predicate;

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
        String normalized = text.trim();
        normalized = normalizeStoreName(normalized);
        return normalized;
    }

    private record OcrRule(String replacement, Predicate<String> condition) {}

    private static final List<OcrRule> OCR_RULES = List.of(
            new OcrRule("Lidl", s -> containsAny(s, "l1dl", "l1d1", "lid1")),
            new OcrRule("Kaufland", s -> containsAny(s, "kauflard", "kauf1and", "kaufl@nd")),
            new OcrRule("Mega Image", s -> containsAny(s, "mega 1mage", "mega lmage", "mega1mage")),
            new OcrRule("Carrefour", s -> containsAny(s, "carref0ur", "carrefour")),
            new OcrRule("Penny", s -> (s.contains("peny") || s.contains("p3nny")) && !s.contains("penny market") && s.length() <= 6),
            new OcrRule("Auchan", s -> containsAny(s, "auch@n", "auch4n")),
            new OcrRule("Profi", s -> containsAny(s, "pr0fi", "prof1")),
            new OcrRule("Selgros", s -> containsAny(s, "s3lgros", "se1gros")),
            new OcrRule("Catena", s -> containsAny(s, "c@tena", "cat3na")),
            new OcrRule("Sensiblu", s -> containsAny(s, "sens1b1u", "sensib1u")),
            new OcrRule("Dona", s -> s.contains("d0na") && s.length() <= 5),
            new OcrRule("Petrom", s -> containsAny(s, "p3trom", "petr0m")),
            new OcrRule("Rompetrol", s -> s.contains("r0mpetr0l")),
            new OcrRule("OMV", s -> s.contains("0mv") && s.length() <= 5)
    );

    private static boolean containsAny(String text, String... patterns) {
        for (String pattern : patterns) {
            if (text.contains(pattern)) {
                return true;
            }
        }
        return false;
    }

    private String normalizeStoreName(String text) {
        if (text == null || text.isBlank()) {
            return text;
        }
        String lower = text.toLowerCase();
        for (OcrRule rule : OCR_RULES) {
            if (rule.condition().test(lower)) {
                return rule.replacement();
            }
        }
        return text;
    }

    private String stripMarkdownFences(String raw) {
        return com.familie.cheltuieli_familie.util.MarkdownUtil.stripMarkdownFences(raw);
    }

    public interface ReceiptExtractor {
        @SystemMessage("""
            Ești un parser inteligent de bonuri fiscale românești și extrase bancare. Primești text brut provenit din OCR și trebuie să extragi informațiile structurate. Textul OCR poate fi fragmentat, murdar sau conține erori de recunoaștere a caracterelor.

            REGULI GENERALE:
            1. Identifică numele magazinului/băncii (storeName) - apare de obicei în partea de sus a bonului. Corectează automat erorile OCR comune.
            2. Identifică suma totală (totalAmount) - caută "TOTAL", "SUMĂ", "TOTAL DE PLATĂ", "PLATĂ", "DE PLATĂ". Ignoră subtotalurile parțiale.
            3. Identifică data (date) - poate fi în orice format românesc cunoscut (dd/MM/yyyy, dd.MM.yyyy, yyyy-MM-dd, dd-MM-yyyy). Dacă anul are doar 2 cifre, presupune 20xx.
            4. Identifică categoria (category) - inferă din tipul magazinului sau articolelor:
               - Supermarketuri (Lidl, Kaufland, Mega Image, Carrefour, Penny, Auchan, Profi, Selgros) → "Mâncare"
               - Benzinării (OMV, Petrom, Rompetrol, Shell, Lukoil, MOL) → "Transport"
               - Farmacii (Catena, Sensiblu, Dona, Help Net, Farmacia Tei) → "Sănătate"
               - Restaurante, fast-food, cafenele → "Divertisment"
               - Magazine de haine (H&M, Zara, C&A, Deichmann, CCC) → "Haine"
               - Facturi utilități (electrică, gaze, apă, internet) → "Utilități"
               - Dacă nu ești sigur, folosește "Diverse"
            5. Lista de articole (items) este opțională. Extrage doar dacă este clară.

            CORECȚII OCR OBLIGATORII - normalizează numele magazinului:
            - "L1dl", "Lidl", "L1d1" → "Lidl"
            - "Kauflard", "Kaufl@nd", "Kauf1and" → "Kaufland"
            - "Mega 1mage", "Mega lmage", "Mega1mage" → "Mega Image"
            - "Carref0ur", "Carrefour", "CarrefOur" → "Carrefour"
            - "Peny", "Penny", "P3nny" → "Penny"
            - "Auch@n", "Auchan", "Auch4n" → "Auchan"
            - "Pr0fi", "Prof1", "Profi" → "Profi"
            - "S3lgros", "Se1gros", "Selgros" → "Selgros"
            - "0MV", "OMV" → "OMV"
            - "P3trom", "Petrom", "Petr0m" → "Petrom"
            - "R0mpetr0l", "Rompetrol" → "Rompetrol"
            - "C@tena", "Cat3na", "Catena" → "Catena"
            - "Sens1b1u", "Sensiblu" → "Sensiblu"
            - "D0na", "Dona" → "Dona"

            SUBSTITUȚII FRECVENTE DE CARACTERE (aplică-le când interpretezi textul):
            - Cifra "1" poate fi litera mică "l" sau mare "I" sau "L"
            - Cifra "0" poate fi litera "O" (mare) sau "o" (mică)
            - Cifra "5" poate fi litera "S" (mare) sau "s" (mică)
            - Cifra "8" poate fi litera "B" (mare) sau "b" (mică)
            - Simbolul "@" poate fi litera "a" sau "A"
            - Cifra "3" poate fi litera mică "e" sau "E" (contextual)
            - Cifra "4" poate fi litera "A" sau "h" (contextual)
            - Litera "m" mică poate fi interpretată greșit ca "rn" sau "n"
            - Textul de pe bonurile termice poate fi fragmentat, cu rânduri rupte sau spații ciudate - reconstruiește cuvintele logic.

            Răspunde EXCLUSIV cu un JSON valid, fără markdown, fără explicații suplimentare. Nu adăuga text înainte sau după JSON.
            Format exact:
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
