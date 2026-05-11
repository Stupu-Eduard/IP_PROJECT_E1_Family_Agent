package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Collections;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class AiExpenseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private ExpenseJpaRepository repository;

    @Test
    void testGetAll() throws Exception {
        Page<ExpenseEntity> page = new PageImpl<>(Collections.singletonList(new ExpenseEntity()));
        when(repository.findAll(any(PageRequest.class))).thenReturn(page);

        mockMvc.perform(get("/v1/expenses"))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON));
    }

    @Test
    void testGetById_Found() throws Exception {
        ExpenseEntity entity = new ExpenseEntity();
        entity.setId(1L);
        when(repository.findById(1L)).thenReturn(Optional.of(entity));

        mockMvc.perform(get("/v1/expenses/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1));
    }

    @Test
    void testGetById_NotFound() throws Exception {
        when(repository.findById(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/v1/expenses/1"))
                .andExpect(status().isNotFound());
    }

    @Test
    void testGetByCategory() throws Exception {
        Page<ExpenseEntity> page = new PageImpl<>(Collections.emptyList());
        when(repository.findByCategory(eq("food"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/expenses/by-category/food"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetByPerson() throws Exception {
        Page<ExpenseEntity> page = new PageImpl<>(Collections.emptyList());
        when(repository.findByPerson(eq("Eu"), any())).thenReturn(page);

        mockMvc.perform(get("/v1/expenses/by-person/Eu"))
                .andExpect(status().isOk());
    }

    @Test
    void testGetByDateRange() throws Exception {
        Page<ExpenseEntity> page = new PageImpl<>(Collections.emptyList());
        when(repository.findByDateRange(any(LocalDate.class), any(LocalDate.class), any())).thenReturn(page);

        mockMvc.perform(get("/v1/expenses/by-date-range")
                .param("from", "2024-01-01")
                .param("to", "2024-01-31"))
                .andExpect(status().isOk());
    }

    @Test
    void testDelete_Success() throws Exception {
        when(repository.existsById(1L)).thenReturn(true);

        mockMvc.perform(delete("/v1/expenses/1"))
                .andExpect(status().isNoContent());
    }

    @Test
    void testDelete_NotFound() throws Exception {
        when(repository.existsById(1L)).thenReturn(false);

        mockMvc.perform(delete("/v1/expenses/1"))
                .andExpect(status().isNotFound());
    }
}
