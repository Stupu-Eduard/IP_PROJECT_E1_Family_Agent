package com.familie.cheltuieli_familie.service;

import org.apache.pdfbox.Loader;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;

@Service
public class TextBasedPdfExtractor {

    public String extractText(File pdfFile) throws IOException {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IllegalArgumentException("Fișierul PDF nu există!");
        }

        try (PDDocument document = Loader.loadPDF(pdfFile)) {
            PDFTextStripper stripper = new PDFTextStripper();
            return stripper.getText(document);
        }
    }

    public boolean isTextBased(File pdfFile) {
        if (pdfFile == null || !pdfFile.exists()) {
            throw new IllegalArgumentException("Fișierul PDF nu există!");
        }

        try {
            String text = extractText(pdfFile);
            return text != null && text.trim().length() > 100;
        } catch (Exception e) {
            return false;
        }
    }
}