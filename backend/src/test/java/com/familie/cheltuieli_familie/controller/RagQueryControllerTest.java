package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.RagRequest;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.RagRetrievalService;
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
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@AutoConfigureMockMvc(addFilters = false)
@WebMvcTest(RagQueryController.class)
@ActiveProfiles("test")
class RagQueryControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private RagQueryController ragQueryController;

    @MockBean
    private RagRetrievalService ragRetrievalService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.JwtAuthFilter jwtAuthFilter;

    @Test
    void testRagQuery() throws Exception {
        when(ragRetrievalService.askWithContext("intrebare")).thenReturn("Raspuns RAG");

        mockMvc.perform(post("/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"intrebare\"}"))
                .andExpect(status().isOk())
                .andExpect(content().string("Raspuns RAG"));
    }

    @Test
    void testRagQueryValidationError() throws Exception {
        mockMvc.perform(post("/v1/rag/query")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"query\": \"\"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void ragQuery_withAuthenticatedUser_appendsIdentityBlock() {
        User user = new User();
        user.setId(2L);
        user.setName("Bob");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ragRetrievalService.askWithContext(anyString())).thenReturn("answer");

        var result = ragQueryController.ragQuery(new RagRequest("question"), auth);

        assertEquals(200, result.getStatusCodeValue());
        verify(ragRetrievalService).askWithContext(contains("[IDENTITATE_AUTENTIFICATA: nume='Bob', user_id=2]"));
    }

    @Test
    void ragQuery_withNullAuthentication_doesNotAppendIdentityBlock() {
        when(ragRetrievalService.askWithContext("question")).thenReturn("answer");

        var result = ragQueryController.ragQuery(new RagRequest("question"), null);

        assertEquals(200, result.getStatusCodeValue());
        verify(ragRetrievalService).askWithContext(eq("question"));
    }

    @Test
    void ragQuery_withNonUserPrincipal_doesNotAppendIdentityBlock() {
        Authentication auth = new UsernamePasswordAuthenticationToken("notAUser", null);

        when(ragRetrievalService.askWithContext("question")).thenReturn("answer");

        var result = ragQueryController.ragQuery(new RagRequest("question"), auth);

        assertEquals(200, result.getStatusCodeValue());
        verify(ragRetrievalService).askWithContext(eq("question"));
    }

    @Test
    void buildIdentityBlock_withNullAuth_returnsEmpty() throws Exception {
        Method method = RagQueryController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        String result = (String) method.invoke(ragQueryController, (Authentication) null);
        assertEquals("", result);
    }

    @Test
    void buildIdentityBlock_withNonUserPrincipal_returnsEmpty() throws Exception {
        Method method = RagQueryController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        Authentication auth = new UsernamePasswordAuthenticationToken("notAUser", null);
        String result = (String) method.invoke(ragQueryController, auth);
        assertEquals("", result);
    }

    @Test
    void buildIdentityBlock_withUserPrincipal_returnsIdentity() throws Exception {
        Method method = RagQueryController.class.getDeclaredMethod("buildIdentityBlock", Authentication.class);
        method.setAccessible(true);

        User user = new User();
        user.setId(99L);
        user.setName("Zoe");
        Authentication auth = new UsernamePasswordAuthenticationToken(user, null);
        String result = (String) method.invoke(ragQueryController, auth);
        assertEquals("[IDENTITATE_AUTENTIFICATA: nume='Zoe', user_id=99] ", result);
    }

    @Test
    void preAuthorizeRequiresAuthentication() {
        PreAuthorize preAuthorize = RagQueryController.class.getAnnotation(PreAuthorize.class);
        assertNotNull(preAuthorize);
        assertEquals("isAuthenticated()", preAuthorize.value());
    }
}
