package com.proiect.m3.extraction.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

@Service
public class PdfExtractionService {

    private static final Logger logger = Logger.getLogger(PdfExtractionService.class.getName());

    public String extractText(MultipartFile file) throws IOException {
        logger.info("Extracting text from PDF: " + file.getOriginalFilename());
        try (PDDocument document = Loader.loadPDF(file.getBytes())) {
            PDFTextStripper stripper = new PDFTextStripper();
            String text = stripper.getText(document);
            logger.fine("Extracted text length: " + text.length());
            return text;
        } catch (IOException e) {
            logger.log(Level.SEVERE, "Failed to extract text from PDF", e);
            throw e;
        }
    }
}
