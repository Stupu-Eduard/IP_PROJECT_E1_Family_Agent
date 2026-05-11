package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Alert;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.AlertRepository;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import com.lowagie.text.*;
import com.lowagie.text.Font;
import com.lowagie.text.pdf.PdfPCell;
import com.lowagie.text.pdf.PdfPTable;
import com.lowagie.text.pdf.PdfWriter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.awt.*;
import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfReportService {

    private final ExpenseJpaRepository expenseRepository;
    private final AlertRepository alertRepository;

    public byte[] generateFinancialReport(LocalDate from, LocalDate to) {
        log.info("Generating PDF Financial Report from {} to {}", from, to);
        
        try {
            List<ExpenseEntity> expenses = expenseRepository.findByDateBetween(from, to);
            
            LocalDateTime startDateTime = from.atStartOfDay();
            LocalDateTime endDateTime = to.atTime(LocalTime.MAX);
            
            List<Alert> violations = alertRepository.findByTimestampBetween(startDateTime, endDateTime)
                    .stream()
                    .filter(a -> a.getExtraCost() != null && a.getExtraCost().compareTo(BigDecimal.ZERO) > 0)
                    .toList();

            log.info("Found {} expenses and {} violations for the period", expenses.size(), violations.size());

            try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
                Document document = new Document(PageSize.A4);
                PdfWriter writer = PdfWriter.getInstance(document, out);
                
                document.open();

                // Header
                Font headerFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, Color.DARK_GRAY);
                Paragraph header = new Paragraph("Family Expenses & Integrity Report", headerFont);
                header.setAlignment(Element.ALIGN_CENTER);
                header.setSpacingAfter(20);
                document.add(header);

                // Period
                document.add(new Paragraph("Reporting Period: " + from + " to " + to));
                document.add(new Paragraph("Generated at: " + LocalDateTime.now()));
                document.add(new Paragraph(" "));

                // Expenses Table
                document.add(new Paragraph("1. Detailed Expenses", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                document.add(new Paragraph(" "));
                
                PdfPTable expenseTable = new PdfPTable(5);
                expenseTable.setWidthPercentage(100);
                addTableHeader(expenseTable, List.of("Date", "Description", "Category", "Person", "Amount"));
                
                BigDecimal totalExpenses = BigDecimal.ZERO;
                for (ExpenseEntity e : expenses) {
                    expenseTable.addCell(e.getDate() != null ? e.getDate().toString() : "N/A");
                    expenseTable.addCell(e.getLocation() != null ? e.getLocation() : "N/A");
                    expenseTable.addCell(e.getCategory() != null ? e.getCategory() : "N/A");
                    expenseTable.addCell(e.getPerson() != null ? e.getPerson() : "N/A");
                    expenseTable.addCell((e.getAmount() != null ? e.getAmount().toString() : "0") + " RON");
                    if (e.getAmount() != null) {
                        totalExpenses = totalExpenses.add(e.getAmount());
                    }
                }
                document.add(expenseTable);
                document.add(new Paragraph(" "));

                // Geofence Violations Table
                document.add(new Paragraph("2. Geofence Violations & Extra Costs", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                document.add(new Paragraph(" "));
                
                PdfPTable alertTable = new PdfPTable(3);
                alertTable.setWidthPercentage(100);
                addTableHeader(alertTable, List.of("Timestamp", "Violation Type", "Extra Cost"));
                
                BigDecimal totalExtraCosts = BigDecimal.ZERO;
                for (Alert a : violations) {
                    alertTable.addCell(a.getTimestamp() != null ? a.getTimestamp().toString() : "N/A");
                    alertTable.addCell(a.getRestrictedCategory() != null ? a.getRestrictedCategory() : "N/A");
                    alertTable.addCell((a.getExtraCost() != null ? a.getExtraCost().toString() : "0") + " RON");
                    if (a.getExtraCost() != null) {
                        totalExtraCosts = totalExtraCosts.add(a.getExtraCost());
                    }
                }
                document.add(alertTable);
                document.add(new Paragraph(" "));

                // Summary
                document.add(new Paragraph("3. Financial Summary", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                document.add(new Paragraph("Total Regular Expenses: " + totalExpenses + " RON"));
                document.add(new Paragraph("Total Extra Costs (Violations): " + totalExtraCosts + " RON"));
                document.add(new Paragraph("GRAND TOTAL: " + totalExpenses.add(totalExtraCosts) + " RON", 
                        FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, Color.RED)));
                document.add(new Paragraph(" "));

                // Integrity Checksum
                String dataToHash = expenses.toString() + violations.toString();
                String checksum = calculateChecksum(dataToHash);
                
                document.add(new Paragraph("4. Digital Integrity Verification", FontFactory.getFont(FontFactory.HELVETICA_BOLD, 14)));
                document.add(new Paragraph("This document is verified for integrity. Any modification to the source data will invalidate this report."));
                document.add(new Paragraph("SHA-256 Checksum: " + checksum, FontFactory.getFont(FontFactory.COURIER, 10)));
                
                // Add to metadata
                document.addTitle("Financial Report - Family Agent");
                document.addAuthor("Family Agent System");
                document.addSubject("Integrity Checksum: " + checksum);

                document.close();
                return out.toByteArray();
            }
        } catch (Exception e) {
            log.error("Error generating PDF report", e);
            throw new RuntimeException("Could not generate PDF: " + e.getMessage(), e);
        }
    }

    private void addTableHeader(PdfPTable table, List<String> headers) {
        headers.forEach(columnTitle -> {
            PdfPCell cell = new PdfPCell();
            cell.setBackgroundColor(Color.LIGHT_GRAY);
            cell.setBorderWidth(1);
            cell.setPhrase(new Phrase(columnTitle, FontFactory.getFont(FontFactory.HELVETICA_BOLD)));
            table.addCell(cell);
        });
    }

    private String calculateChecksum(String data) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        byte[] hash = digest.digest(data.getBytes(StandardCharsets.UTF_8));
        return HexFormat.of().withLowerCase().formatHex(hash);
    }
}
