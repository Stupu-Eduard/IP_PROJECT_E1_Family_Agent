package com.familie.cheltuieli_familie.service;

import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@Service
public class BankOcrService {
    private final OCRPreProcessor preProcessor;
    private final BankingDictionaryCorrector corrector;
    private static final Logger logger = LoggerFactory.getLogger(BankOcrService.class);

    public BankOcrService(OCRPreProcessor preProcessor, BankingDictionaryCorrector corrector) {
        this.preProcessor = preProcessor;
        this.corrector = corrector;
    }

    public String extractText(File pdfFile, String bank) throws IOException, TesseractException {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            ITesseract tesseract = new Tesseract();

            tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata/");
            tesseract.setLanguage("ron");

            StringBuilder result = new StringBuilder();

            java.nio.file.Path secureTempDir = Files.createTempDirectory("bank_ocr_");

            try {
                for (int page = 0; page < document.getNumberOfPages(); page++) {
                    BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                    java.nio.file.Path tempPath = Files.createTempFile(secureTempDir, "page_" + page, ".png");
                    File tempFile = tempPath.toFile();

                    try {
                        javax.imageio.ImageIO.write(image, "png", tempFile);
                        BufferedImage processed = preProcessor.processImage(tempFile, bank);
                        String pageText = tesseract.doOCR(processed);
                        String correctedText = corrector.correctText(pageText);
                        result.append(correctedText).append("\n");
                        logger.info("Processed page: {}", page);
                    } finally {
                        Files.deleteIfExists(tempPath);
                    }
                }
            } finally {
                org.springframework.util.FileSystemUtils.deleteRecursively(secureTempDir);
            }

            return result.toString();
        }
    }
}