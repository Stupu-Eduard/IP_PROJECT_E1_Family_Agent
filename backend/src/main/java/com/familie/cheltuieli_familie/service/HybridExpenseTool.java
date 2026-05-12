package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import dev.langchain4j.agent.tool.Tool;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Component
@Slf4j
@RequiredArgsConstructor
public class HybridExpenseTool {

    private final QdrantVectorService qdrantVectorService;
    private final ExpenseJpaRepository expenseJpaRepository;

    @Tool("Search for expenses semantically similar to a description, then return their total amount and count from the database.")
    public String searchSimilarAndAggregate(String description, String from, String to) {
        log.info("Hybrid tool: searchSimilarAndAggregate for '{}', from {} to {}", description, from, to);

        List<EmbeddedExpense> similar = qdrantVectorService.searchSimilar(description, 20);
        List<Long> ids = similar.stream()
                .map(EmbeddedExpense::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        if (ids.isEmpty()) {
            return "Nu s-au găsit cheltuieli similare semantic.";
        }

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        List<ExpenseEntity> dbRecords = expenseJpaRepository.findAllById(ids);
        BigDecimal total = dbRecords.stream()
                .filter(e -> e.getDate() != null)
                .filter(e -> !e.getDate().isBefore(fromDate) && !e.getDate().isAfter(toDate))
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        long count = dbRecords.stream()
                .filter(e -> e.getDate() != null)
                .filter(e -> !e.getDate().isBefore(fromDate) && !e.getDate().isAfter(toDate))
                .count();

        return String.format("S-au găsit %d cheltuieli similare. Total în perioada %s–%s: %s RON (%d cheltuieli).",
                ids.size(), from, to, total, count);
    }

    @Tool("Compare the semantic total (similar expenses) vs the exact database total for a category or description.")
    public String compareSemanticVsDbTotal(String description, String from, String to) {
        log.info("Hybrid tool: compareSemanticVsDbTotal for '{}', from {} to {}", description, from, to);

        List<EmbeddedExpense> similar = qdrantVectorService.searchSimilar(description, 20);
        List<Long> ids = similar.stream()
                .map(EmbeddedExpense::getId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();

        LocalDate fromDate = LocalDate.parse(from);
        LocalDate toDate = LocalDate.parse(to);

        BigDecimal semanticTotal = BigDecimal.ZERO;
        if (!ids.isEmpty()) {
            List<ExpenseEntity> dbRecords = expenseJpaRepository.findAllById(ids);
            semanticTotal = dbRecords.stream()
                    .filter(e -> e.getDate() != null)
                    .filter(e -> !e.getDate().isBefore(fromDate) && !e.getDate().isAfter(toDate))
                    .map(ExpenseEntity::getAmount)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
        }

        List<ExpenseEntity> allInRange = expenseJpaRepository.findByDateBetween(fromDate, toDate);
        BigDecimal dbTotal = allInRange.stream()
                .map(ExpenseEntity::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        return String.format(
                "Comparare pentru '%s' în perioada %s–%s: total semantic (cheltuieli similare) = %s RON, total exact din bază de date = %s RON.",
                description, from, to, semanticTotal, dbTotal);
    }
}
