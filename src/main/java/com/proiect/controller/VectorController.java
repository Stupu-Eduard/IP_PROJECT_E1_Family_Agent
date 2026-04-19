package com.proiect.controller;

import com.proiect.repository.ExpenseVectorRepositoryImpl;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/v1/vectors")
public class VectorController {

    private final ExpenseVectorRepositoryImpl repository;

    public VectorController(ExpenseVectorRepositoryImpl repository) {
        this.repository = repository;
    }

    @GetMapping("/check/{id}")
    public boolean checkVectorExists(@PathVariable Long id) {
        return repository.existsInVectorStore(id);
    }
}
