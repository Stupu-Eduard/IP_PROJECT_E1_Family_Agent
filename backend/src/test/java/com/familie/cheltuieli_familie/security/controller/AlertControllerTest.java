package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.controller.AlertController;
import com.familie.cheltuieli_familie.security.service.AlertService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;

import java.util.List;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = AlertController.class)
@AutoConfigureMockMvc(addFilters = false)
class AlertControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AlertService alertService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.SessionCookieFilter sessionCookieFilter;

    @Test
    void testGetAlerts() throws Exception {
        Long parentId = 1L;
        when(alertService.getAlertsForParent(parentId)).thenReturn(List.of());

        // Apelăm URL-ul corect cu Query Parameter (?parentId=1)
        mockMvc.perform(get("/api/v1/alerts")
                        .param("parentId", parentId.toString()))
                .andExpect(status().isOk());

        verify(alertService).getAlertsForParent(parentId);
    }

    @Test
    void testGetUnreadAlerts() throws Exception {
        Long parentId = 1L;
        when(alertService.getUnreadAlertsForParent(parentId)).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/alerts/unread")
                        .param("parentId", parentId.toString()))
                .andExpect(status().isOk());

        verify(alertService).getUnreadAlertsForParent(parentId);
    }

    @Test
    void testMarkAsRead() throws Exception {
        Long alertId = 99L;

        // Folosim PATCH așa cum este definit în Controller (@PatchMapping)
        mockMvc.perform(patch("/api/v1/alerts/" + alertId + "/read"))
                .andExpect(status().isOk());

        verify(alertService).markAsRead(alertId);
    }
}