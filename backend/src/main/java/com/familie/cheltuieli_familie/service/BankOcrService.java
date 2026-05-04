package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

@Slf4j
@Service
public class BankOcrService {
    private final OCRPreProcessor preProcessor;
    private final BankingDictionaryCorrector corrector;

    public BankOcrService(OCRPreProcessor preProcessor, BankingDictionaryCorrector corrector) {
        this.preProcessor = preProcessor;
        this.corrector = corrector;
    }

    private File createSecureTempFile(String prefix, String suffix) throws IOException {
        Path projectRoot = Paths.get(System.getProperty("user.dir"));
        Path secureDir = Files.createTempDirectory(projectRoot, "secure-temp");
        Path secureFile = Files.createTempFile(secureDir, prefix, suffix);
        return secureFile.toFile();
    }

    public String extractText(File pdfFile, String bank) throws Exception {
        try (PDDocument document = Loader.loadPDF(pdfFile)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);
            ITesseract tesseract = new Tesseract();

            tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata/");
            tesseract.setLanguage("ron");

            StringBuilder result = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {
                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);
                File tempFile = createSecureTempFile("page_" + page, ".png");
                javax.imageio.ImageIO.write(image, "png", tempFile);
                BufferedImage processed = preProcessor.processImage(tempFile, bank);
                String pageText = tesseract.doOCR(processed);
                String correctedText = corrector.correctText(pageText);
                result.append(correctedText).append("\n");
                File parentDir = tempFile.getParentFile();
                boolean isFileDeleted = tempFile.delete();
                if(!isFileDeleted){
                    log.warn("Atentie: Nu s-a putut sterge fisierul temporar {}", tempFile.getAbsolutePath());
                }
                if(parentDir != null){
                    boolean isDirDeleted = parentDir.delete();
                    if (!isDirDeleted) {
                        log.warn("Atentie: Nu s-a putut sterge directorul temporar {}", parentDir.getAbsolutePath());
                    }
                }
                System.out.println("Processed page: " + page);
            }
            return result.toString();
        }
    }
}