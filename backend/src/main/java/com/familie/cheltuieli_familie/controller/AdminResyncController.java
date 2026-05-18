package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ResyncStatusDto;
import com.familie.cheltuieli_familie.service.QdrantResyncService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
public class AdminResyncController {

    private final QdrantResyncService qdrantResyncService;

    public AdminResyncController(QdrantResyncService qdrantResyncService) {
        this.qdrantResyncService = qdrantResyncService;
    }

    @PostMapping("/resync/qdrant")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<ResyncStatusDto> resyncQdrant(@RequestParam(required = false) Long familyId) {
        QdrantResyncService.ResyncResult result;
        if (familyId != null) {
            result = qdrantResyncService.resyncExpensesForFamily(familyId);
        } else {
            result = qdrantResyncService.resyncAllExpenses();
        }
        return ResponseEntity.ok(new ResyncStatusDto(result.processedCount(), result.errorCount()));
    }
}
