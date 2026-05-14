package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository.ExpenseWithLocationProjection;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.apache.pdfbox.pdmodel.font.Standard14Fonts;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

@Service
@Slf4j
@RequiredArgsConstructor
public class PdfExportService {

    private final ExpenseRepository      expenseRepository;
    private final FamilyMemberRepository familyMemberRepository;

    private static final DateTimeFormatter RO_DATE      = DateTimeFormatter.ofPattern("dd.MM.yyyy");
    private static final float             MARGIN        = 50f;
    private static final float             PAGE_WIDTH    = PDRectangle.A4.getWidth();
    private static final float             PAGE_HEIGHT   = PDRectangle.A4.getHeight();
    private static final float             CONTENT_WIDTH = PAGE_WIDTH - 2 * MARGIN;

    public byte[] generatePdf(LocalDate from, LocalDate to, Authentication auth) throws IOException {
        log.info("Generating PDF for period {} - {}", from, to);

        // ── Fetch cheltuieli exact ca ExpenseController ───────────────────────
        List<ExpenseWithLocationProjection> expenses;
        if (auth != null && auth.getPrincipal() instanceof User user) {
            boolean isParent = auth.getAuthorities().stream()
                    .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT")
                            || a.getAuthority().equals("ROLE_CO-PARENT"));

            if (isParent) {
                expenses = familyMemberRepository.findByUserId(user.getId()).stream()
                        .findFirst()
                        .map(fm -> expenseRepository.findAllByFamilyFiltered(
                                fm.getFamily().getId(), null, null, null))
                        .orElseGet(() -> expenseRepository.findAllByUserFiltered(
                                user.getId(), null, null));
            } else {
                expenses = expenseRepository.findAllByUserFiltered(user.getId(), null, null);
            }
        } else {
            expenses = List.of();
        }

        // ── Filtrare pe perioadă ──────────────────────────────────────────────
        List<ExpenseWithLocationProjection> filtered = expenses.stream()
                .filter(e -> e.getExpenseDate() != null)
                .filter(e -> {
                    LocalDate d = e.getExpenseDate().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .toList();

        log.info("Found {} expenses for period {} - {}", filtered.size(), from, to);

        // ── Grupare adaptivă ──────────────────────────────────────────────────
        long diffDays = ChronoUnit.DAYS.between(from, to) + 1;
        LinkedHashMap<String, BigDecimal> groupedMap = new LinkedHashMap<>();

        if (diffDays <= 31) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
            LocalDate cursor = from;
            while (!cursor.isAfter(to)) {
                groupedMap.put(cursor.format(fmt), BigDecimal.ZERO);
                cursor = cursor.plusDays(1);
            }
            filtered.forEach(e -> {
                String key = e.getExpenseDate().toLocalDate().format(fmt);
                groupedMap.computeIfPresent(key, (k, v) -> v.add(e.getAmount()));
            });
        } else if (diffDays <= 90) {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
            filtered.forEach(e -> {
                LocalDate monday = e.getExpenseDate().toLocalDate().with(DayOfWeek.MONDAY);
                groupedMap.merge(monday.format(fmt), e.getAmount(), BigDecimal::add);
            });
        } else {
            DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM.yyyy");
            filtered.forEach(e -> {
                String key = e.getExpenseDate().toLocalDate().format(fmt);
                groupedMap.merge(key, e.getAmount(), BigDecimal::add);
            });
        }

        List<String>     labels = new ArrayList<>(groupedMap.keySet());
        List<BigDecimal> values = new ArrayList<>(groupedMap.values());

        BigDecimal total = filtered.stream()
                .map(ExpenseWithLocationProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal average = diffDays > 0
                ? total.divide(new BigDecimal(diffDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        // ── Generare PDF ──────────────────────────────────────────────────────
        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;

                // Header
                cs.setFont(fontBold, 18);
                cs.setNonStrokingColor(0.18f, 0.18f, 0.18f);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Evolutie Cheltuieli");
                cs.endText();
                y -= 22;

                cs.setFont(fontRegular, 10);
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, y);
                cs.showText("Perioada: " + from.format(RO_DATE) + " - " + to.format(RO_DATE)
                        + "   |   Generat: " + LocalDate.now().format(RO_DATE));
                cs.endText();
                y -= 6;

                cs.setStrokingColor(0.88f, 0.88f, 0.88f);
                cs.setLineWidth(1f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 20;

                // Grafic
                if (!labels.isEmpty()) {
                    BigDecimal maxVal = values.stream().max(BigDecimal::compareTo).orElse(BigDecimal.ONE);
                    if (maxVal.compareTo(BigDecimal.ZERO) == 0) maxVal = BigDecimal.ONE;

                    int   barCount   = labels.size();
                    float chartH     = 130f;
                    float barSpacing = CONTENT_WIDTH / barCount;
                    float barWidth   = Math.min(barSpacing * 0.5f, 28f);
                    float chartBaseY = y - chartH;

                    cs.setStrokingColor(0.8f, 0.8f, 0.8f);
                    cs.setLineWidth(0.5f);
                    cs.moveTo(MARGIN, chartBaseY);
                    cs.lineTo(PAGE_WIDTH - MARGIN, chartBaseY);
                    cs.stroke();

                    for (int i = 0; i < barCount; i++) {
                        BigDecimal val = values.get(i);
                        float ratio = val.divide(maxVal, 4, RoundingMode.HALF_UP).floatValue();
                        float barH2 = Math.max(ratio * chartH, val.compareTo(BigDecimal.ZERO) > 0 ? 4f : 0f);
                        float barX  = MARGIN + i * barSpacing + (barSpacing - barWidth) / 2f;

                        if (barH2 > 0) {
                            cs.setNonStrokingColor(0.53f, 0.36f, 0.22f);
                            cs.addRect(barX, chartBaseY, barWidth, barH2);
                            cs.fill();
                        }

                        if (val.compareTo(BigDecimal.ZERO) > 0) {
                            cs.setFont(fontRegular, 7);
                            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
                            String valStr = val.setScale(0, RoundingMode.HALF_UP).toPlainString();
                            cs.beginText();
                            cs.newLineAtOffset(barX + barWidth / 2f - (valStr.length() * 2.2f), chartBaseY + barH2 + 3f);
                            cs.showText(valStr);
                            cs.endText();
                        }

                        if (barCount <= 52) {
                            String lbl = labels.get(i);
                            cs.setFont(fontRegular, 7);
                            cs.setNonStrokingColor(0.55f, 0.55f, 0.55f);
                            cs.beginText();
                            cs.newLineAtOffset(barX + barWidth / 2f - (lbl.length() * 2f), chartBaseY - 12f);
                            cs.showText(lbl);
                            cs.endText();
                        }
                    }
                    y = chartBaseY - 35f;
                } else {
                    cs.setFont(fontRegular, 11);
                    cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                    cs.beginText();
                    cs.newLineAtOffset(MARGIN, y - 60f);
                    cs.showText("Nu exista cheltuieli in aceasta perioada.");
                    cs.endText();
                    y -= 90f;
                }

                // KPI
                cs.setStrokingColor(0.88f, 0.88f, 0.88f);
                cs.setLineWidth(0.5f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 18;

                float      kpiColW = CONTENT_WIDTH / 3f;
                String[][] kpis    = {
                        {"TOTAL PERIOADA", total.setScale(2, RoundingMode.HALF_UP).toPlainString() + " RON"},
                        {"MEDIE ZILNICA",  average.toPlainString() + " RON"},
                        {"TRANZACTII",     String.valueOf(filtered.size())}
                };

                for (int i = 0; i < kpis.length; i++) {
                    float kpiX = MARGIN + i * kpiColW;
                    cs.setFont(fontRegular, 8);
                    cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
                    cs.beginText();
                    cs.newLineAtOffset(kpiX, y);
                    cs.showText(kpis[i][0]);
                    cs.endText();

                    cs.setFont(fontBold, 14);
                    cs.setNonStrokingColor(0.18f, 0.18f, 0.18f);
                    cs.beginText();
                    cs.newLineAtOffset(kpiX, y - 16);
                    cs.showText(kpis[i][1]);
                    cs.endText();
                }
                y -= 42;

                // Tabel
                cs.setStrokingColor(0.88f, 0.88f, 0.88f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 16;

                float[]  cols    = {MARGIN, MARGIN + 80, MARGIN + 230, MARGIN + 330, MARGIN + 430};
                String[] headers = {"DATA", "CATEGORIE", "DESCRIERE", "PERSOANA", "SUMA (RON)"};

                cs.setFont(fontBold, 9);
                cs.setNonStrokingColor(0.5f, 0.5f, 0.5f);
                for (int i = 0; i < headers.length; i++) {
                    cs.beginText();
                    cs.newLineAtOffset(cols[i], y);
                    cs.showText(headers[i]);
                    cs.endText();
                }
                y -= 6;
                cs.setStrokingColor(0.75f, 0.75f, 0.75f);
                cs.moveTo(MARGIN, y);
                cs.lineTo(PAGE_WIDTH - MARGIN, y);
                cs.stroke();
                y -= 13;

                List<ExpenseWithLocationProjection> sorted = filtered.stream()
                        .sorted((a, b) -> b.getExpenseDate().compareTo(a.getExpenseDate()))
                        .toList();

                boolean alt = false;
                for (ExpenseWithLocationProjection e : sorted) {
                    if (y < 60) break;
                    if (alt) {
                        cs.setNonStrokingColor(0.97f, 0.96f, 0.95f);
                        cs.addRect(MARGIN, y - 3, CONTENT_WIDTH, 14);
                        cs.fill();
                    }
                    alt = !alt;

                    cs.setFont(fontRegular, 9);
                    cs.setNonStrokingColor(0.25f, 0.25f, 0.25f);
                    String[] row = {
                            e.getExpenseDate().toLocalDate().format(RO_DATE),
                            truncate(e.getCategory(), 18),
                            truncate(e.getDescription(), 22),
                            truncate(e.getPerson(), 14),
                            e.getAmount().setScale(2, RoundingMode.HALF_UP).toPlainString()
                    };
                    for (int i = 0; i < row.length; i++) {
                        cs.beginText();
                        cs.newLineAtOffset(cols[i], y);
                        cs.showText(row[i] != null ? row[i] : "-");
                        cs.endText();
                    }
                    y -= 14;
                }

                // Footer
                cs.setFont(fontRegular, 8);
                cs.setNonStrokingColor(0.7f, 0.7f, 0.7f);
                cs.beginText();
                cs.newLineAtOffset(MARGIN, 30);
                cs.showText("FamilyAgent - Raport generat automat | " + LocalDate.now().format(RO_DATE));
                cs.endText();
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private String truncate(String s, int max) {
        if (s == null) return "-";
        return s.length() > max ? s.substring(0, max - 1) + "..." : s;
    }
}