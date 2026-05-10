package com.proiect.controller;
import org.springframework.boot.test.context.SpringBootTest;
import com.familie.cheltuieli_familie.repository.UserSessionRepository;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

import com.proiect.model.ExpenseEntity;
import com.proiect.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.PageImpl;
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
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.MOCK)
@ActiveProfiles("test")
@AutoConfigureMockMvc(addFilters = false)
class ExpenseControllerTest {

    @MockBean
    private UserSessionRepository userSessionRepository;

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

    @Test
    void testGetByCategory() throws Exception {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .category("Food")
                .date(LocalDate.now())
                .build();

        when(repository.findByCategory(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(expense)));

        mockMvc.perform(get("/v1/expenses/by-category/Food")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].category").value("Food"));
    }

    @Test
    void testGetByPerson() throws Exception {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .person("Teodor")
                .date(LocalDate.now())
                .build();

        when(repository.findByPerson(any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(expense)));

        mockMvc.perform(get("/v1/expenses/by-person/Teodor")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].person").value("Teodor"));
    }

    @Test
    void testGetByDateRange() throws Exception {
        ExpenseEntity expense = ExpenseEntity.builder()
                .id(1L)
                .amount(new BigDecimal("100.00"))
                .date(LocalDate.now())
                .build();

        when(repository.findByDateRange(any(), any(), any(Pageable.class))).thenReturn(new PageImpl<>(List.of(expense)));

        mockMvc.perform(get("/v1/expenses/by-date-range")
                .param("from", "2024-01-01")
                .param("to", "2024-01-31")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content[0].amount").value(100.00));
    }

    @Test
    void testDeleteSuccess() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);
        doNothing().when(repository).deleteById(1L);

        mockMvc.perform(delete("/v1/expenses/1")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDeleteNotFound() throws Exception {
        when(repository.existsById(99L)).thenReturn(false);

        mockMvc.perform(delete("/v1/expenses/99")
                .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());
    }
}
