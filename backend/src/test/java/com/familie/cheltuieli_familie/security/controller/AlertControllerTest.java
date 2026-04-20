package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.controller.AlertController;
import com.familie.cheltuieli_familie.security.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private AlertService alertService;

    @Test
    void testGetAlerts() throws Exception {
        when(alertService.getAlertsForParent(1L)).thenReturn(List.of());

        mockMvc.perform(get("/api/alerts/1"))
                .andExpect(status().isOk());

        verify(alertService).getAlertsForParent(1L);
    }

    @Test
    void testMarkAsRead() throws Exception {
        mockMvc.perform(put("/api/alerts/99/read"))
                .andExpect(status().isOk());

        verify(alertService).markAsRead(99L);
    }
}