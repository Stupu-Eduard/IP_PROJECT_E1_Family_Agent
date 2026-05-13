package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.service.QdrantVectorService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vectors")
@PreAuthorize("isAuthenticated()")
public class VectorController {

    private final QdrantVectorService service;

    public VectorController(QdrantVectorService service) {
        this.service = service;
    }

    @GetMapping("/check/{id}")
    public ResponseEntity<Boolean> checkVectorExists(@PathVariable Long id) {
        return ResponseEntity.ok(service.existsInVectorStore(id));
    }
}
