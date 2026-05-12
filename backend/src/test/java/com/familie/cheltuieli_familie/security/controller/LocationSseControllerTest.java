package com.familie.cheltuieli_familie.security.controller;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = LocationSseController.class)
@AutoConfigureMockMvc(addFilters = false)
class LocationSseControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private LocationStreamService locationStreamService;

    @MockBean
    private com.familie.cheltuieli_familie.security.filter.SessionCookieFilter sessionCookieFilter;

    @Test
    void testStreamLocation() throws Exception {
        Long parentId = 1L;
        when(locationStreamService.subscribeParent(parentId)).thenReturn(new SseEmitter());

        mockMvc.perform(get("/api/v1/parent/location-stream")
                        .param("parentId", parentId.toString()))
                .andExpect(status().isOk());
    }
}
