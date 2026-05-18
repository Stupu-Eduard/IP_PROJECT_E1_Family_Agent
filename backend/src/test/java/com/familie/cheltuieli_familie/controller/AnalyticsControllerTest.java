package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.AnalyticsAssistant;
import com.familie.cheltuieli_familie.service.ReportService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(AnalyticsController.class)
@ActiveProfiles("test")
class AnalyticsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private AnalyticsController analyticsController;

    @MockBean
    private AnalyticsAssistant analyticsAssistant;

    @MockBean
    private ReportService reportService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testQuery() throws Exception {
        when(analyticsAssistant.chat(anyString(), anyString())).thenReturn("Ai cheltuit 100 lei.");

        mockMvc.perform(post("/v1/analytics/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("\"Cât am cheltuit?\""))
                .andExpect(status().isOk())
                .andExpect(content().string("Ai cheltuit 100 lei."));
    }

    @Test
    void testGetNarrativeReport() throws Exception {
        when(reportService.generateNarrativeReport(2024, 3)).thenReturn("Raport lunar: ai cheltuit 500 RON.");

        mockMvc.perform(get("/v1/analytics/report/2024/3"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raport lunar: ai cheltuit 500 RON."));
    }

    @Test
    void query_withAuthenticatedUser_appendsIdentityBlock() {
        User user = new User();
        user.setId(1L);
        user.setName("Test");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        when(analyticsAssistant.chat(anyString(), anyString())).thenReturn("response");

        var result = analyticsController.query("hello", auth);

        assertEquals(200, result.getStatusCode().value());
        verify(analyticsAssistant).chat(contains("[IDENTITATE_AUTENTIFICATA: nume='Test', user_id=1]"), anyString());
    }

    @Test
    void query_withNullAuthentication_doesNotAppendIdentityBlock() {
        when(analyticsAssistant.chat(eq("hello"), anyString())).thenReturn("response");

        var result = analyticsController.query("hello", null);

        assertEquals(200, result.getStatusCode().value());
        verify(analyticsAssistant).chat(eq("hello"), anyString());
    }

    @Test
    void query_withNonUserPrincipal_doesNotAppendIdentityBlock() {
        Authentication auth = new UsernamePasswordAuthenticationToken("stringPrincipal", null);

        when(analyticsAssistant.chat(eq("hello"), anyString())).thenReturn("response");

        var result = analyticsController.query("hello", auth);

        assertEquals(200, result.getStatusCode().value());
        verify(analyticsAssistant).chat(eq("hello"), anyString());
    }

    @Test
    void buildIdentityBlock_withNullAuth_returnsEmpty() throws Exception {
        Method method = AnalyticsController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        String result = (String) method.invoke(analyticsController, (Authentication) null);
        assertEquals("", result);
    }

    @Test
    void buildIdentityBlock_withNonUserPrincipal_returnsEmpty() throws Exception {
        Method method = AnalyticsController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        Authentication auth = new UsernamePasswordAuthenticationToken("notAUser", null);
        String result = (String) method.invoke(analyticsController, auth);
        assertEquals("", result);
    }

    @Test
    void buildIdentityBlock_withUserPrincipal_returnsIdentity() throws Exception {
        Method method = AnalyticsController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        User user = new User();
        user.setId(42L);
        user.setName("Alice");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
        String result = (String) method.invoke(analyticsController, auth);
        assertEquals("[IDENTITATE_AUTENTIFICATA: nume='Alice', user_id=42] ", result);
    }

    @Test
    void getNarrativeReport_withDifferentYearMonth() throws Exception {
        when(reportService.generateNarrativeReport(2023, 12)).thenReturn("Raport decembrie");

        mockMvc.perform(get("/v1/analytics/report/2023/12"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raport decembrie"));
    }

    @Test
    void preAuthorizeRequiresAuthentication() {
        PreAuthorize preAuthorize = AnalyticsController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
