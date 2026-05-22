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

    // ── Public entry point ────────────────────────────────────────────────────

    public byte[] generatePdf(LocalDate from, LocalDate to, Authentication auth) throws IOException {
        log.info("Generating PDF for period {} - {}", from, to);

        List<ExpenseWithLocationProjection> filtered = fetchAndFilter(from, to, auth);
        log.info("Found {} expenses for period {} - {}", filtered.size(), from, to);

        long diffDays = ChronoUnit.DAYS.between(from, to) + 1;
        List<String>     labels = new ArrayList<>();
        List<BigDecimal> values = new ArrayList<>();
        buildChartData(filtered, from, to, diffDays, labels, values);

        BigDecimal total   = sumTotal(filtered);
        BigDecimal average = diffDays > 0
                ? total.divide(new BigDecimal(diffDays), 2, RoundingMode.HALF_UP)
                : BigDecimal.ZERO;

        return renderPdf(from, to, filtered, labels, values, total, average);
    }

    // ── Data fetching ─────────────────────────────────────────────────────────

    private List<ExpenseWithLocationProjection> fetchAndFilter(
            LocalDate from, LocalDate to, Authentication auth) {

        if (auth == null || !(auth.getPrincipal() instanceof User user)) {
            return List.of();
        }

        boolean isParent = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_PARENT")
                        || a.getAuthority().equals("ROLE_CO-PARENT"));

        List<ExpenseWithLocationProjection> all = isParent
                ? fetchForParent(user)
                : expenseRepository.findAllByUserFiltered(user.getId(), null, null);

        return all.stream()
                .filter(e -> e.getExpenseDate() != null)
                .filter(e -> {
                    LocalDate d = e.getExpenseDate().toLocalDate();
                    return !d.isBefore(from) && !d.isAfter(to);
                })
                .toList();
    }

    private List<ExpenseWithLocationProjection> fetchForParent(User user) {
        return familyMemberRepository.findByUserId(user.getId()).stream()
                .findFirst()
                .map(fm -> expenseRepository.findAllByFamilyFiltered(
                        fm.getFamily().getId(), null, null, null))
                .orElseGet(() -> expenseRepository.findAllByUserFiltered(
                        user.getId(), null, null));
    }

    // ── Chart data ────────────────────────────────────────────────────────────

    private void buildChartData(
            List<ExpenseWithLocationProjection> expenses,
            LocalDate from, LocalDate to, long diffDays,
            List<String> labels, List<BigDecimal> values) {

        LinkedHashMap<String, BigDecimal> map;
        if (diffDays <= 31) {
            map = buildDailyMap(expenses, from, to);
        } else if (diffDays <= 90) {
            map = buildWeeklyMap(expenses);
        } else {
            map = buildMonthlyMap(expenses);
        }

        labels.addAll(map.keySet());
        values.addAll(map.values());
    }

    private LinkedHashMap<String, BigDecimal> buildDailyMap(
            List<ExpenseWithLocationProjection> expenses, LocalDate from, LocalDate to) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        LinkedHashMap<String, BigDecimal> map = new LinkedHashMap<>();
        for (LocalDate d = from; !d.isAfter(to); d = d.plusDays(1)) {
            map.put(d.format(fmt), BigDecimal.ZERO);
        }
        expenses.forEach(e -> {
            String key = e.getExpenseDate().toLocalDate().format(fmt);
            map.computeIfPresent(key, (k, v) -> v.add(e.getAmount()));
        });
        return map;
    }

    private LinkedHashMap<String, BigDecimal> buildWeeklyMap(
            List<ExpenseWithLocationProjection> expenses) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd.MM");
        LinkedHashMap<String, BigDecimal> map = new LinkedHashMap<>();
        expenses.forEach(e -> {
            LocalDate monday = e.getExpenseDate().toLocalDate().with(DayOfWeek.MONDAY);
            map.merge(monday.format(fmt), e.getAmount(), BigDecimal::add);
        });
        return map;
    }

    private LinkedHashMap<String, BigDecimal> buildMonthlyMap(
            List<ExpenseWithLocationProjection> expenses) {

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("MM.yyyy");
        LinkedHashMap<String, BigDecimal> map = new LinkedHashMap<>();
        expenses.forEach(e -> {
            String key = e.getExpenseDate().toLocalDate().format(fmt);
            map.merge(key, e.getAmount(), BigDecimal::add);
        });
        return map;
    }

    private BigDecimal sumTotal(List<ExpenseWithLocationProjection> expenses) {
        return expenses.stream()
                .map(ExpenseWithLocationProjection::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    // ── PDF rendering ─────────────────────────────────────────────────────────

    private byte[] renderPdf(
            LocalDate from, LocalDate to,
            List<ExpenseWithLocationProjection> expenses,
            List<String> labels, List<BigDecimal> values,
            BigDecimal total, BigDecimal average) throws IOException {

        try (PDDocument doc = new PDDocument()) {
            PDPage page = new PDPage(PDRectangle.A4);
            doc.addPage(page);

            PDType1Font fontBold    = new PDType1Font(Standard14Fonts.FontName.HELVETICA_BOLD);
            PDType1Font fontRegular = new PDType1Font(Standard14Fonts.FontName.HELVETICA);

            try (PDPageContentStream cs = new PDPageContentStream(doc, page)) {
                float y = PAGE_HEIGHT - MARGIN;
                y = drawHeader(cs, fontBold, fontRegular, from, to, y);
                y = drawChart(cs, fontRegular, labels, values, y);
                y = drawKpis(cs, fontBold, fontRegular, total, average, expenses.size(), y);
                drawTable(cs, fontBold, fontRegular, expenses, y);
                drawFooter(cs, fontRegular);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            doc.save(out);
            return out.toByteArray();
        }
    }

    private float drawHeader(PDPageContentStream cs,
                             PDType1Font fontBold, PDType1Font fontRegular,
                             LocalDate from, LocalDate to, float y) throws IOException {

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

        return y - 20;
    }

    private float drawChart(PDPageContentStream cs,
                            PDType1Font fontRegular,
                            List<String> labels, List<BigDecimal> values,
                            float y) throws IOException {

        if (labels.isEmpty()) {
            cs.setFont(fontRegular, 11);
            cs.setNonStrokingColor(0.6f, 0.6f, 0.6f);
            cs.beginText();
            cs.newLineAtOffset(MARGIN, y - 60f);
            cs.showText("Nu exista cheltuieli in aceasta perioada.");
            cs.endText();
            return y - 90f;
        }

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
            drawBar(cs, fontRegular, new BarConfig(
                    labels.get(i), values.get(i), maxVal,
                    chartH, chartBaseY, barSpacing, barWidth, i, barCount));
        }

        return chartBaseY - 35f;
    }

    private record BarConfig(
            String label, BigDecimal val, BigDecimal maxVal,
            float chartH, float chartBaseY,
            float barSpacing, float barWidth, int index, int barCount) {}

    private void drawBar(PDPageContentStream cs, PDType1Font fontRegular,
                         BarConfig b) throws IOException {

        float ratio = b.val().divide(b.maxVal(), 4, RoundingMode.HALF_UP).floatValue();
        float barH  = Math.max(ratio * b.chartH(), b.val().compareTo(BigDecimal.ZERO) > 0 ? 4f : 0f);
        float barX  = MARGIN + b.index() * b.barSpacing() + (b.barSpacing() - b.barWidth()) / 2f;

        if (barH > 0) {
            cs.setNonStrokingColor(0.53f, 0.36f, 0.22f);
            cs.addRect(barX, b.chartBaseY(), b.barWidth(), barH);
            cs.fill();
        }

        if (b.val().compareTo(BigDecimal.ZERO) > 0) {
            cs.setFont(fontRegular, 7);
            cs.setNonStrokingColor(0.3f, 0.3f, 0.3f);
            String valStr = b.val().setScale(0, RoundingMode.HALF_UP).toPlainString();
            cs.beginText();
            cs.newLineAtOffset(barX + b.barWidth() / 2f - (valStr.length() * 2.2f), b.chartBaseY() + barH + 3f);
            cs.showText(valStr);
            cs.endText();
        }

        if (b.barCount() <= 52) {
            cs.setFont(fontRegular, 7);
            cs.setNonStrokingColor(0.55f, 0.55f, 0.55f);
            cs.beginText();
            cs.newLineAtOffset(barX + b.barWidth() / 2f - (b.label().length() * 2f), b.chartBaseY() - 12f);
            cs.showText(b.label());
            cs.endText();
        }
    }

    private float drawKpis(PDPageContentStream cs,
                           PDType1Font fontBold, PDType1Font fontRegular,
                           BigDecimal total, BigDecimal average, int count, float y) throws IOException {

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
                {"TRANZACTII",     String.valueOf(count)}
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

        return y - 42;
    }

    private void drawTable(PDPageContentStream cs,
                           PDType1Font fontBold, PDType1Font fontRegular,
                           List<ExpenseWithLocationProjection> expenses, float y) throws IOException {

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

        List<ExpenseWithLocationProjection> sorted = expenses.stream()
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
    }

    private void drawFooter(PDPageContentStream cs, PDType1Font fontRegular) throws IOException {
        cs.setFont(fontRegular, 8);
        cs.setNonStrokingColor(0.7f, 0.7f, 0.7f);
        cs.beginText();
        cs.newLineAtOffset(MARGIN, 30);
        cs.showText("FamilyAgent - Raport generat automat | " + LocalDate.now().format(RO_DATE));
        cs.endText();
    }

    private String truncate(String s, int max) {
        if (s == null) return "-";
        String normalized = normalizeRo(s).replace("\n", " ").replace("\r", " ");
        return normalized.length() > max ? normalized.substring(0, max - 1) + "..." : normalized;
    }

    private String normalizeRo(String s) {
        return s.replace("ă", "a").replace("Ă", "A")
                .replace("â", "a").replace("Â", "A")
                .replace("î", "i").replace("Î", "I")
                .replace("ș", "s").replace("Ș", "S")
                .replace("ş", "s").replace("Ş", "S")
                .replace("ț", "t").replace("Ț", "T")
                .replace("ţ", "t").replace("Ţ", "T");
    }
}