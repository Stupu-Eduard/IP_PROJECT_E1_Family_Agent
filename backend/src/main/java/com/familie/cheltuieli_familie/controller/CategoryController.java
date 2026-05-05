package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/v1/categories")
@CrossOrigin(origins = {"https://family-agent.me", "http://localhost:5173"})
public class CategoryController {

    private final CategoryRepository categoryRepository;

    public CategoryController(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @GetMapping
    public List<String> list() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"))
                .stream()
                .map(Category::getName)
                .filter(name -> name != null && !name.trim().isEmpty())
                .toList();
    }
}
