package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Service
@Slf4j
public class PdfExtractionService {

    public String extractText(MultipartFile file) throws IOException {
        log.info("Extracting text from PDF: {}", file.getOriginalFilename());
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            log.debug("Extracted text length: {}", text.length());
            return text;
        } catch (IOException e) {
            log.error("Failed to extract text from PDF", e);
            throw e;
        }
    }
}
