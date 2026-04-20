package com.proiect.controller;

import com.proiect.service.QdrantVectorService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vectors")
public class VectorController {

    private final QdrantVectorService service;

    public VectorController(QdrantVectorService service) {
        this.service = service;
    }

    @GetMapping("/check/{id}")
    public boolean checkVectorExists(@PathVariable Long id) {
        return service.existsInVectorStore(id);
    }
}
