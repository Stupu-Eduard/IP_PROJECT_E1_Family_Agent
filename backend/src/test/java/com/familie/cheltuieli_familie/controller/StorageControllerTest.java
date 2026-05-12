package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.service.StorageService;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class StorageControllerTest {

    private final StorageService storageService = mock(StorageService.class);
    private final StorageController controller = new StorageController(storageService);
    private final MockMvc mockMvc = MockMvcBuilders.standaloneSetup(controller).build();

    @Test
    void saveTransactionsShouldReturnStorageResult() throws Exception {

        StorageResult result = new StorageResult(1, 1, 0);

        when(storageService.save(anyList())).thenReturn(result);

        mockMvc.perform(post("/api/storage/transactions")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                    [
                                        {
                                           "date": "2025-03-10",
                                           "amount": 100.5,
                                           "description": "Lidl",
                                           "type": "EXPENSE",
                                           "currency": "RON"
                                        }
                                    ]
                                """))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.totalTransactions").value(1))
                .andExpect(jsonPath("$.savedTransactions").value(1))
                .andExpect(jsonPath("$.failedTransactions").value(0));

        verify(storageService, times(1)).save(anyList());
    }
}
