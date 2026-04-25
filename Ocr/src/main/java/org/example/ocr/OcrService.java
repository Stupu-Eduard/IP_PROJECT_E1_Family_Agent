package org.example.ocr;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.apache.pdfbox.Loader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;

@Service
public class OcrService {

    private static final Logger log = LoggerFactory.getLogger(OcrService.class);

    @Value("${tesseract.datapath}")
    private String tessDataPath;

    private final ITesseract tesseract = new Tesseract();

    @PostConstruct
    public void init() {
        tesseract.setDatapath(tessDataPath);
        tesseract.setLanguage("eng");
    }

    public String extractTextFromPdf(File pdfFile) {

        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            StringBuilder result = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {

                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);

                String pageText = tesseract.doOCR(image);
                result.append(pageText).append("\n");

                log.info("Processed page {}", page);
            }

            return result.toString();

        } catch (IOException | TesseractException e) {
            log.error("OCR failed", e);
            throw new RuntimeException("Failed to process PDF for OCR", e);
        }
    }
}