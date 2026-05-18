package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.dto.FilterRequest;
import com.familie.cheltuieli_familie.dto.SearchRequest;
import com.familie.cheltuieli_familie.service.QdrantVectorService;
import com.familie.cheltuieli_familie.service.SearchQueryCorrector;
import com.familie.cheltuieli_familie.service.SemanticExpansionService;
import com.familie.cheltuieli_familie.security.util.SecurityService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.math.BigDecimal;
import java.sql.Date;
import java.time.LocalDate;
import java.util.*;

@RestController
@RequestMapping("/v1/search")
@Slf4j
@RequiredArgsConstructor
@PreAuthorize("isAuthenticated()")
public class ExpenseSearchController {

    private final QdrantVectorService qdrantVectorService;
    private final SearchQueryCorrector searchQueryCorrector;
    private final SemanticExpansionService semanticExpansionService;
    private final JdbcTemplate jdbcTemplate;
    private final SecurityService securityService;

    private static final int MIN_RESULTS_THRESHOLD = 3;

    @PostMapping
    public ResponseEntity<List<EmbeddedExpense>> semanticSearch(@Valid @RequestBody SearchRequest request) {
        String originalQuery = request.getQuery();
        log.info("Received semantic search request: {}", originalQuery);

        // 1. Apply typo correction
        String correctedQuery = searchQueryCorrector.correctQuery(originalQuery);

        // 2. Vector search with user/family scope
        Long[] scope = securityService.resolveScope();
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(correctedQuery, request.getTopK(), scope[0], scope[1]);

        // 3. Semantic expansion fallback if too few results
        if (results.size() < MIN_RESULTS_THRESHOLD) {
            List<String> expandedCategories = semanticExpansionService.expandCategories(correctedQuery);
            for (String cat : expandedCategories) {
                if (results.size() >= MIN_RESULTS_THRESHOLD) break;
                List<EmbeddedExpense> catResults = qdrantVectorService.searchSimilar(cat, request.getTopK(), scope[0], scope[1]);
                for (EmbeddedExpense r : catResults) {
                    if (results.stream().noneMatch(e -> Objects.equals(e.getId(), r.getId()))) {
                        results.add(r);
                    }
                }
            }
        }

        // 4. SQL keyword fallback if still too few results
        if (results.size() < MIN_RESULTS_THRESHOLD) {
            List<EmbeddedExpense> keywordResults = searchByKeyword(correctedQuery, request.getTopK());
            for (EmbeddedExpense r : keywordResults) {
                if (results.stream().noneMatch(e -> Objects.equals(e.getId(), r.getId()))) {
                    results.add(r);
                }
            }
        }

        // Sort by score descending
        results = new ArrayList<>(results);
        results.sort(Comparator.comparingDouble(EmbeddedExpense::getScore).reversed());
        if (results.size() > request.getTopK()) {
            results = results.subList(0, request.getTopK());
        }

        return ResponseEntity.ok(results);
    }

    @PostMapping("/filter")
    public ResponseEntity<List<EmbeddedExpense>> filteredSearch(@Valid @RequestBody FilterRequest request) {
        log.info("Received filtered search request: {}", request);
        String correctedQuery = searchQueryCorrector.correctQuery(request.getQuery());
        Long[] scope = securityService.resolveScope();
        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                correctedQuery,
                request.getTopK(),
                request.getCategory(),
                request.getPerson(),
                request.getFrom(),
                request.getTo(),
                scope[0],
                scope[1]
        );
        return ResponseEntity.ok(results);
    }

    private List<EmbeddedExpense> searchByKeyword(String query, int topK) {
        try {
            String pattern = "%" + query.toLowerCase() + "%";
            Long[] scope = securityService.resolveScope();
            Long familyId = scope[0];
            Long userId = scope[1];

            StringBuilder sql = new StringBuilder("""
                SELECT e.id, e.amount, e.description, e.expense_date,
                       c.name as category, u.name as person, l.store as location, e.raw_input
                FROM expenses e
                LEFT JOIN categories c ON e.category_id = c.id
                LEFT JOIN users u ON e.user_id = u.id
                LEFT JOIN locations l ON e.location_id = l.id
                WHERE (LOWER(e.description) LIKE ? OR LOWER(c.name) LIKE ? OR LOWER(u.name) LIKE ?
                   OR LOWER(l.store) LIKE ? OR LOWER(e.raw_input) LIKE ?)
                """);

            List<Object> params = new ArrayList<>(List.of(pattern, pattern, pattern, pattern, pattern));

            if (familyId != null) {
                sql.append(" AND e.family_id = ?");
                params.add(familyId);
            } else if (userId != null) {
                sql.append(" AND e.user_id = ?");
                params.add(userId);
            }

            sql.append(" ORDER BY e.expense_date DESC LIMIT ?");
            params.add(topK);

            return jdbcTemplate.query(sql.toString(), (rs, rowNum) -> {
                Long id = rs.getLong("id");
                BigDecimal amount = rs.getBigDecimal("amount");
                String category = rs.getString("category");
                String person = rs.getString("person");
                String location = rs.getString("location");
                Date dateSql = rs.getDate("expense_date");
                LocalDate date = dateSql != null ? dateSql.toLocalDate() : null;
                String rawInput = rs.getString("raw_input");
                return EmbeddedExpense.builder()
                        .id(id)
                        .amount(amount)
                        .category(category)
                        .person(person)
                        .location(location)
                        .date(date)
                        .rawInput(rawInput)
                        .score(0.0)
                        .build();
            }, params.toArray());
        } catch (Exception e) {
            log.warn("Keyword search failed: {}", e.getMessage());
            return List.of();
        }
    }
}
