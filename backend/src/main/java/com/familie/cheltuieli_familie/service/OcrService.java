package com.familie.cheltuieli_familie.service;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.text.PDFTextStripper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.familie.cheltuieli_familie.exception.AiServiceException;
import jakarta.annotation.PostConstruct;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    @Value("${ocr.language:ron+eng}")
    private String ocrLanguage;

    private final ITesseract tesseract = new Tesseract();

    @PostConstruct
    public void init() {
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage(ocrLanguage);
    }

    public String extractTextFromPdf(File pdfFile) {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            // First try direct text extraction (for digital PDFs like bank statements)
            String directText = extractTextDirectly(document);
            if (directText != null && !directText.isBlank() && directText.trim().length() > 50) {
                log.info("Extracted {} characters directly from digital PDF: {}", directText.length(), pdfFile.getName());
                return directText;
            }

            // Fall back to OCR for scanned/image-based PDFs
            log.info("Direct text extraction yielded little content, falling back to OCR for: {}", pdfFile.getName());
            return extractTextWithOcr(document);

        } catch (IOException e) {
            log.error("Failed to load PDF: {}", pdfFile.getName(), e);
            throw new AiServiceException("Failed to process PDF", e);
        }
    }

    private String extractTextDirectly(PDDocument document) {
        try {
            PDFTextStripper stripper = new PDFTextStripper();
            stripper.setSortByPosition(true);
            return stripper.getText(document);
        } catch (IOException e) {
            log.warn("Direct text extraction failed: {}", e.getMessage());
            return null;
        }
    }

    private String extractTextWithOcr(PDDocument document) {
        try {
            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                String pageText = tesseract.doOCR(image);
                result.append(pageText).append("\n");
                log.info("OCR processed page {}", page);
            }
            return result.toString();
        } catch (IOException | TesseractException e) {
            log.error("OCR failed", e);
            throw new AiServiceException("Failed to process PDF for OCR", e);
        }
    }

    public String extractTextFromImage(File imageFile) {
        try {
            String text = tesseract.doOCR(imageFile);
            log.info("OCR extracted {} characters from image: {}", text.length(), imageFile.getName());
            return text;
        } catch (TesseractException e) {
            log.error("OCR failed for image: {}", imageFile.getName(), e);
            throw new AiServiceException("Failed to process image for OCR", e);
        }
    }
}
