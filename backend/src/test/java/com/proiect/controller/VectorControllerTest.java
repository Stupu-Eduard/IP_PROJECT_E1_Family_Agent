package com.proiect.controller;

import com.proiect.service.QdrantVectorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(VectorController.class)
@ActiveProfiles("test")
class VectorControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private QdrantVectorService qdrantVectorService;

    @Test
    void testCheckVectorExistsTrue() throws Exception {
        when(qdrantVectorService.existsInVectorStore(1L)).thenReturn(true);

        mockMvc.perform(get("/v1/vectors/check/1"))
                .andExpect(status().isOk())
                .andExpect(content().string("true"));
    }

    @Test
    void testCheckVectorExistsFalse() throws Exception {
        when(qdrantVectorService.existsInVectorStore(99L)).thenReturn(false);

        mockMvc.perform(get("/v1/vectors/check/99"))
                .andExpect(status().isOk())
                .andExpect(content().string("false"));
    }
}
