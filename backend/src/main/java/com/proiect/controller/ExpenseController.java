package com.proiect.controller;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;

@RestController
@RequestMapping("/v1/expenses")
@Slf4j
@RequiredArgsConstructor
public class ExpenseController {

    private final ExpenseJpaRepository repository;

    @GetMapping
    public ResponseEntity<Page<ExpenseEntity>> getAll(Pageable pageable) {
        log.info("Fetching all expenses, pageable: {}", pageable);
        return ResponseEntity.ok(repository.findAll(pageable));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ExpenseEntity> getById(@PathVariable Long id) {
        log.info("Fetching expense with id: {}", id);
        return repository.findById(id)
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/by-category/{cat}")
    public ResponseEntity<Page<ExpenseEntity>> getByCategory(@PathVariable String cat, Pageable pageable) {
        log.info("Fetching expenses for category: {}, pageable: {}", cat, pageable);
        return ResponseEntity.ok(repository.findByCategory(cat, pageable));
    }

    @GetMapping("/by-person/{person}")
    public ResponseEntity<Page<ExpenseEntity>> getByPerson(@PathVariable String person, Pageable pageable) {
        log.info("Fetching expenses for person: {}, pageable: {}", person, pageable);
        return ResponseEntity.ok(repository.findByPerson(person, pageable));
    }

    @GetMapping("/by-date-range")
    public ResponseEntity<Page<ExpenseEntity>> getByDateRange(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to,
            Pageable pageable) {
        log.info("Fetching expenses from {} to {}, pageable: {}", from, to, pageable);
        return ResponseEntity.ok(repository.findByDateRange(from, to, pageable));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        log.info("Deleting expense with id: {}", id);
        if (!repository.existsById(id)) {
            return ResponseEntity.notFound().build();
        }
        repository.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
