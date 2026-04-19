package com.proiect.controller;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ExpenseController.class)
@ActiveProfiles("test")
class ExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseJpaRepository repository;

    @Test
    void testGetAll() throws Exception {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .date(LocalDate.now())
                .build();
        
        when(repository.findAll(any(Pageable.class))).thenReturn(new PageImpl<>(List.of(expense)));

        mockMvc.perform(get("/v1/expenses")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(100.00))
                .andExpect(jsonPath("$.content[0].category").value("Food"));
    }

    @Test
    void testGetById() throws Exception {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .date(LocalDate.now())
                .build();

        when(repository.findById(1L)).thenReturn(Optional.of(expense));

        mockMvc.perform(get("/v1/expenses/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.amount").value(100.00))
                .andExpect(jsonPath("$.category").value("Food"));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/expenses/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
