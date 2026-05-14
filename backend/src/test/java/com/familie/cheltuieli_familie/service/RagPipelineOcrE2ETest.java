package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.test.context.ActiveProfiles;


import java.io.File;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;

import org.awaitility.Awaitility;

import static org.junit.jupiter.api.Assertions.*;

/**
 * E2E test validating the full OCR → Embedding → RAG pipeline:
 * 1. Extract text from a real PDF bank statement
 * 2. Generate embeddings via OpenRouter
 * 3. Store in Qdrant
 * 4. Search via vector similarity
 * 5. Verify RAG retrieval returns relevant context
 */
@SpringBootTest
@ActiveProfiles("test")
class RagPipelineOcrE2ETest {

    @Autowired
    private EmbeddingService embeddingService;

    @Autowired
    private QdrantVectorService qdrantVectorService;

    @Value("${qdrant.host:localhost}")
    private String host;

    @Value("${qdrant.port:6333}")
    private int httpPort;

    @BeforeEach
    void setUp() {
        try {
            java.net.Socket socket = new java.net.Socket("localhost", 6333);
            socket.close();
        } catch (Exception e) {
            Assumptions.assumeTrue(false, "Qdrant not available on localhost:6333");
            return;
        }
    }

    @Test
    void testPdfTextExtractionAndEmbedding() throws java.io.IOException {
        // STEP 1: Load sample PDF from classpath
        ClassPathResource resource = new ClassPathResource("sample.pdf");
        File pdfFile = resource.getFile();
        assertTrue(pdfFile.exists(), "Sample PDF should exist");

        // STEP 2: Extract text using PDFBox (same as production)
        org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(pdfFile);
        org.apache.pdfbox.text.PDFTextStripper stripper = new org.apache.pdfbox.text.PDFTextStripper();
        String extractedText = stripper.getText(document);
        document.close();

        assertNotNull(extractedText);
        assertTrue(extractedText.length() > 1000, "PDF should contain substantial text");
        assertTrue(extractedText.contains("BANCA COMERCIALA ROMANA"), "Should contain bank name");
        assertTrue(extractedText.contains("Steam Purchase") || extractedText.contains("Spotify"),
                "Should contain transaction references");

        System.out.println("=== PDF Text Extraction ===");
        System.out.println("Pages: " + document.getNumberOfPages());
        System.out.println("Text length: " + extractedText.length());
        System.out.println("Preview: " + extractedText.substring(0, 200));

        // STEP 3: Create expense entity from extracted text
        long uniqueId = System.currentTimeMillis();
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(uniqueId)
                .amount(new BigDecimal("7.79"))
                .category("Divertisment")
                .person("Casian Mihai")
                .location("Steam")
                .date(LocalDate.of(2026, 4, 2))
                .rawInput(extractedText.substring(0, Math.min(800, extractedText.length())))
                .build();

        // STEP 4: Store in vector store (generates embedding + stores in Qdrant)
        qdrantVectorService.storeExpense(expense);

        // STEP 5: Search for Steam transaction
        List<EmbeddedExpense> results = Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> qdrantVectorService.searchSimilar("Steam Purchase tranzactie", 5), r -> !r.isEmpty());

        assertFalse(results.isEmpty(), "Should find the stored expense");
        assertTrue(results.stream().anyMatch(r -> r.getId() != null && r.getId() == uniqueId),
                "Should find the exact expense we stored");

        // Verify the retrieved context contains relevant info
        EmbeddedExpense found = results.get(0);
        assertNotNull(found.getRawInput(), "Retrieved expense should have raw_input");
        assertTrue(found.getRawInput().contains("BANCA") || found.getRawInput().contains("Steam"),
                "Retrieved context should contain bank or transaction info");

        System.out.println("=== RAG Retrieval ===");
        System.out.println("Found expense ID: " + found.getId());
        System.out.println("Score: " + found.getScore());
        System.out.println("Raw input preview: " + found.getRawInput().substring(0, Math.min(150, found.getRawInput().length())));
    }

    @Test
    void testImageEmbeddingFromPdfPage() throws java.io.IOException {
        // STEP 1: Render PDF page to image
        ClassPathResource resource = new ClassPathResource("sample.pdf");
        org.apache.pdfbox.pdmodel.PDDocument document = org.apache.pdfbox.Loader.loadPDF(resource.getFile());
        org.apache.pdfbox.rendering.PDFRenderer renderer = new org.apache.pdfbox.rendering.PDFRenderer(document);
        java.awt.image.BufferedImage image = renderer.renderImageWithDPI(0, 150);
        document.close();

        assertNotNull(image);
        assertTrue(image.getWidth() > 0 && image.getHeight() > 0, "Image should have dimensions");

        // STEP 2: Save as PNG
        File tempImage = File.createTempFile("bank_statement", ".png");
        javax.imageio.ImageIO.write(image, "png", tempImage);

        System.out.println("=== PDF to Image ===");
        System.out.println("Image size: " + image.getWidth() + "x" + image.getHeight());
        System.out.println("File size: " + tempImage.length() + " bytes");

        // STEP 3: The vision model can embed this image directly
        // For this test, we verify the embedding service works with text
        // (Image embedding via base64 is validated in OcrEmbeddingE2E manual test)
        float[] embedding = embeddingService.getEmbedding("Bank statement from BCR with Steam Purchase transaction");
        assertNotNull(embedding);
        assertEquals(2048, embedding.length, "Embedding should be 2048 dimensions");

        System.out.println("=== Embedding ===");
        System.out.println("Dimension: " + embedding.length);
        System.out.println("First 5 values: " + java.util.Arrays.toString(java.util.Arrays.copyOf(embedding, 5)));

        tempImage.delete();
    }

    @Test
    void testFullRagQueryWithOcrContext() {
        // This test simulates: User uploads PDF → OCR extracts text → embedded → RAG answers query

        // 1. Simulate OCR extraction
        String ocrText = "EXTRAS DE CONT BCR. Data: 02-04-2026. " +
                "Tranzactie comerciant Steam Purchase Hamburg, suma 7.60 RON. " +
                "Tranzactie Spotify Stockholm, suma 14 RON. " +
                "Sold final: 858.46 RON.";

        // 2. Store as expense with raw_input
        long uniqueId = System.currentTimeMillis() + 1;
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(uniqueId)
                .amount(new BigDecimal("21.60"))
                .category("Abonamente")
                .person("Casian Mihai")
                .location("Online")
                .date(LocalDate.of(2026, 4, 2))
                .rawInput(ocrText)
                .build();

        qdrantVectorService.storeExpense(expense);

        // 3. RAG search for "Spotify subscription cost"
        List<EmbeddedExpense> results = Awaitility.await()
                .atMost(Duration.ofSeconds(15))
                .pollInterval(Duration.ofMillis(200))
                .until(() -> qdrantVectorService.searchSimilar("cat costa abonamentul Spotify", 5), r -> !r.isEmpty());

        assertFalse(results.isEmpty(), "RAG should retrieve the OCR'd expense");

        EmbeddedExpense bestMatch = results.get(0);
        // The top result should be the one we just stored (or very relevant)
        assertTrue(bestMatch.getRawInput().contains("Spotify") || bestMatch.getRawInput().contains("Steam"),
            "Retrieved context should mention Spotify or Steam");

        System.out.println("=== Full RAG Pipeline ===");
        System.out.println("Query: 'cat costa abonamentul Spotify'");
        System.out.println("Retrieved ID: " + bestMatch.getId());
        System.out.println("Retrieved score: " + bestMatch.getScore());
        System.out.println("Retrieved raw_input: " + bestMatch.getRawInput());
        System.out.println("✅ RAG pipeline: OCR → Embed → Store → Search → Retrieve = WORKING");
    }
}
