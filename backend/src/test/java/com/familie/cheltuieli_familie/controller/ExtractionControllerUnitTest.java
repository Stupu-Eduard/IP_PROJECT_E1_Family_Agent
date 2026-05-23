package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.CloudinaryService;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.ExtractionService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.core.Authentication;

import java.io.File;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ExtractionControllerUnitTest {

    @Mock private ExtractionService extractionService;
    @Mock private ExtractionPipelineService orchestrator;
    @Mock private CloudinaryService cloudinaryService;

    @InjectMocks
    private ExtractionController controller;

    @Test
    void processDocument_withAuthenticatedUser_uploadsToCloudinaryAndSetsReceiptUrl() throws Exception {
        User user = new User();
        user.setId(7L);
        user.setEmail("auth@test.com");
        user.setName("AuthUser");
        user.setPasswordH("hash");

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        MockMultipartFile dummyFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[10]);

        Transaction tx = new Transaction(LocalDate.now(), "Mega", 75.0, "RON", "EXPENSE");
        when(orchestrator.processDocument(any(File.class), anyString()))
                .thenReturn(List.of(tx));
        when(cloudinaryService.uploadFile(any(File.class), anyString(), anyString()))
                .thenReturn("https://cloudinary.com/doc.pdf");

        ResponseEntity<List<Transaction>> response = controller.processDocument(dummyFile, "ING", auth);

        assertEquals(200, response.getStatusCode().value());
        verify(cloudinaryService).uploadFile(any(File.class), anyString(), anyString());
        assertNotNull(response.getBody());
        assertEquals("https://cloudinary.com/doc.pdf", response.getBody().get(0).getReceiptUrl());
    }

    @Test
    void processDocument_withNullAuthentication_skipsCloudinaryUpload() throws Exception {
        MockMultipartFile dummyFile = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[10]);

        Transaction tx = new Transaction(LocalDate.now(), "Carrefour", 20.0, "RON", "EXPENSE");
        when(orchestrator.processDocument(any(File.class), anyString()))
                .thenReturn(List.of(tx));

        ResponseEntity<List<Transaction>> response = controller.processDocument(dummyFile, "revolut", null);

        assertEquals(200, response.getStatusCode().value());
        verifyNoInteractions(cloudinaryService);
        assertNull(response.getBody().get(0).getReceiptUrl());
    }
}
