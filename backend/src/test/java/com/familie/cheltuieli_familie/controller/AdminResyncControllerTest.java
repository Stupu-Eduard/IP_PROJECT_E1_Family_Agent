package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.ResyncStatusDto;
import com.familie.cheltuieli_familie.service.QdrantResyncService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AdminResyncController.class)
@ActiveProfiles("test")
class AdminResyncControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AdminResyncController adminResyncController;

    @MockBean
    private QdrantResyncService qdrantResyncService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void resyncQdrant_withoutFamilyId_returnsOk() throws Exception {
        when(qdrantResyncService.resyncAllExpenses()).thenReturn(new QdrantResyncService.ResyncResult(100, 2));

        mockMvc.perform(post("/api/v1/admin/resync/qdrant")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(100))
                .andExpect(jsonPath("$.errorCount").value(2));
    }

    @Test
    void resyncQdrant_withFamilyId_returnsOk() throws Exception {
        when(qdrantResyncService.resyncExpensesForFamily(5L)).thenReturn(new QdrantResyncService.ResyncResult(50, 0));

        mockMvc.perform(post("/api/v1/admin/resync/qdrant")
                        .param("familyId", "5")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.processedCount").value(50))
                .andExpect(jsonPath("$.errorCount").value(0));
    }

    @Test
    void resyncQdrant_serviceThrowsException_propagatesError() {
        when(qdrantResyncService.resyncAllExpenses()).thenThrow(new RuntimeException("DB error"));

        assertThrows(RuntimeException.class, () -> adminResyncController.resyncQdrant(null));
    }

    @Test
    void resyncQdrant_withFamilyId_serviceThrowsException_propagatesError() {
        when(qdrantResyncService.resyncExpensesForFamily(5L)).thenThrow(new RuntimeException("Family not found"));

        assertThrows(RuntimeException.class, () -> adminResyncController.resyncQdrant(5L));
    }

    @Test
    void preAuthorizeRequiresAdminRole() throws NoSuchMethodException {
        java.lang.reflect.Method method = AdminResyncController.class.getMethod("resyncQdrant", Long.class);
        PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("hasRole('ADMIN')", preAuthorize.value());
    }

    @Test
    void resyncQdrant_directCall_withoutFamilyId() {
        when(qdrantResyncService.resyncAllExpenses()).thenReturn(new QdrantResyncService.ResyncResult(10, 1));

        var result = adminResyncController.resyncQdrant(null);

        assertEquals(200, result.getStatusCodeValue());
        ResyncStatusDto body = result.getBody();
        assertNotNull(body);
        assertEquals(10, body.processedCount());
        assertEquals(1, body.errorCount());
        verify(qdrantResyncService).resyncAllExpenses();
        verifyNoMoreInteractions(qdrantResyncService);
    }

    @Test
    void resyncQdrant_directCall_withFamilyId() {
        when(qdrantResyncService.resyncExpensesForFamily(3L)).thenReturn(new QdrantResyncService.ResyncResult(5, 0));

        var result = adminResyncController.resyncQdrant(3L);

        assertEquals(200, result.getStatusCodeValue());
        ResyncStatusDto body = result.getBody();
        assertNotNull(body);
        assertEquals(5, body.processedCount());
        assertEquals(0, body.errorCount());
        verify(qdrantResyncService).resyncExpensesForFamily(3L);
        verifyNoMoreInteractions(qdrantResyncService);
    }

    @Test
    void resyncQdrant_directCall_zeroResults() {
        when(qdrantResyncService.resyncAllExpenses()).thenReturn(new QdrantResyncService.ResyncResult(0, 0));

        var result = adminResyncController.resyncQdrant(null);

        assertEquals(200, result.getStatusCodeValue());
        ResyncStatusDto body = result.getBody();
        assertNotNull(body);
        assertEquals(0, body.processedCount());
        assertEquals(0, body.errorCount());
    }
}
