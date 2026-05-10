package com.proiect.service;

import com.proiect.dto.EmbeddedExpense;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class SemanticExpansionService {

    private final QdrantVectorService qdrantVectorService;
    private static final int DEFAULT_TOP_K = 20;

    /**
     * Expands a fuzzy category description into concrete category strings
     * by searching Qdrant for semantically similar expenses.
     *
     * Example: "mall shopping" → ["shopping", "haine", "brand-uri"]
     */
    public List<String> expandCategories(String fuzzyCategory) {
        if (fuzzyCategory == null || fuzzyCategory.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(fuzzyCategory, DEFAULT_TOP_K);
            List<String> categories = results.stream()
                    .map(EmbeddedExpense::getCategory)
                    .filter(Objects::nonNull)
                    .filter(c -> !c.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            log.info("Expanded category '{}' into {} concrete categories: {}",
                    fuzzyCategory, categories.size(), categories);
            return categories;

        } catch (Exception e) {
            log.warn("Semantic expansion failed for category '{}', falling back to exact match: {}",
                    fuzzyCategory, e.getMessage());
            return List.of(fuzzyCategory);
        }
    }

    /**
     * Expands a fuzzy location description into concrete location strings.
     */
    public List<String> expandLocations(String fuzzyLocation) {
        if (fuzzyLocation == null || fuzzyLocation.isBlank()) {
            return Collections.emptyList();
        }

        try {
            List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(fuzzyLocation, DEFAULT_TOP_K);
            List<String> locations = results.stream()
                    .map(EmbeddedExpense::getLocation)
                    .filter(Objects::nonNull)
                    .filter(l -> !l.isBlank())
                    .distinct()
                    .collect(Collectors.toList());

            log.info("Expanded location '{}' into {} concrete locations: {}",
                    fuzzyLocation, locations.size(), locations);
            return locations;

        } catch (Exception e) {
            log.warn("Semantic expansion failed for location '{}', falling back to exact match: {}",
                    fuzzyLocation, e.getMessage());
            return List.of(fuzzyLocation);
        }
    }
}
