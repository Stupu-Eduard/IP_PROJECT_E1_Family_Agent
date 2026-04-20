package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(LocationSseController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private LocationStreamService locationStreamService;

    @Test
    void testSubscribe() throws Exception {
        when(locationStreamService.subscribeParent(1L)).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/security/location/subscribe/1"))
                .andExpect(status().isOk());
    }
}