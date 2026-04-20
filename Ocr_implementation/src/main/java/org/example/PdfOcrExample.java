package org.example;
import net.sourceforge.tess4j.ITesseract;
import net.sourceforge.tess4j.Tesseract;
import net.sourceforge.tess4j.TesseractException;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.rendering.PDFRenderer;

import java.awt.image.BufferedImage;
import java.io.File;

public class PdfOcrExample {

    public static void main(String[] args) {
        File pdfFile = new File("input.pdf");

        try (PDDocument document = PDDocument.load(pdfFile)) {

            PDFRenderer pdfRenderer = new PDFRenderer(document);

            ITesseract tesseract = new Tesseract();

            // Set path to tessdata (important!)
            tesseract.setDatapath("/usr/share/tesseract-ocr/5/tessdata/");
            tesseract.setLanguage("eng");

            StringBuilder result = new StringBuilder();

            for (int page = 0; page < document.getNumberOfPages(); page++) {

                BufferedImage image = pdfRenderer.renderImageWithDPI(page, 300);

                String pageText = tesseract.doOCR(image);
                result.append(pageText).append("\n");

                System.out.println("Processed page: " + page);
            }

            System.out.println("Final OCR Result:\n");
            System.out.println(result.toString());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}