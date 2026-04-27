package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.EmbeddedExpense;
import com.familie.cheltuieli_familie.dto.FilterRequest;
import com.familie.cheltuieli_familie.dto.SearchRequest;
import com.familie.cheltuieli_familie.service.QdrantVectorService;
import lombok.RequiredArgsConstructor;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/v1/search")
@Slf4j
@RequiredArgsConstructor
public class ExpenseSearchController {

    private final QdrantVectorService qdrantVectorService;

    @PostMapping
    public ResponseEntity<List<EmbeddedExpense>> semanticSearch(@Valid @RequestBody SearchRequest request) {
        log.info("Received semantic search request: {}", request.getQuery());
        List<EmbeddedExpense> results = qdrantVectorService.searchSimilar(request.getQuery(), request.getTopK());
        return ResponseEntity.ok(results);
    }

    @PostMapping("/filter")
    public ResponseEntity<List<EmbeddedExpense>> filteredSearch(@Valid @RequestBody FilterRequest request) {
        log.info("Received filtered search request: {}", request);
        List<EmbeddedExpense> results = qdrantVectorService.searchWithFilter(
                request.getQuery(),
                request.getTopK(),
                request.getCategory(),
                request.getPerson(),
                request.getFrom(),
                request.getTo()
        );
        return ResponseEntity.ok(results);
    }
}
