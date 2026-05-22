package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.service.CloudinaryService;
import com.familie.cheltuieli_familie.service.ExtractionPipelineService;
import com.familie.cheltuieli_familie.service.StorageService;
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
class ExtractionOCRControllerUnitTest {

    @Mock private ExtractionPipelineService extractionPipelineService;
    @Mock private StorageService storageService;
    @Mock private CloudinaryService cloudinaryService;
    @Mock private com.familie.cheltuieli_familie.security.util.SecurityService securityService;

    @InjectMocks
    private ExtractionOCRController controller;

    @Test
    void extractAndSave_withAuthenticatedUser_uploadsToCloudinaryAndSetsReceiptUrl() throws Exception {
        User user = new User();
        user.setId(42L);
        user.setEmail("user@test.com");
        user.setName("TestUser");
        user.setPasswordH("hash");

        when(securityService.getCurrentUser()).thenReturn(user);

        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", "content".getBytes());

        Transaction tx = new Transaction(LocalDate.now(), "Kaufland", 50.0, "RON", "EXPENSE");
        when(extractionPipelineService.processDocument(any(File.class), any()))
                .thenReturn(List.of(tx));
        when(storageService.save(any())).thenReturn(new StorageResult(1, 1, 0));
        when(cloudinaryService.uploadFile(any(File.class), anyString(), anyString()))
                .thenReturn("https://cloudinary.com/receipt.pdf");

        ResponseEntity<List<Transaction>> response = controller.extractAndSave(fakePdf, "ing", null);

        assertEquals(200, response.getStatusCode().value());
        verify(cloudinaryService).uploadFile(any(File.class), anyString(), anyString());
        assertNotNull(response.getBody());
        assertEquals("https://cloudinary.com/receipt.pdf", response.getBody().get(0).getReceiptUrl());
        assertEquals(42L, response.getBody().get(0).getUserId());
    }

    @Test
    void extractAndSave_withNullAuthentication_returnsUnauthorized() throws Exception {
        when(securityService.getCurrentUser()).thenReturn(null);

        MockMultipartFile fakePdf = new MockMultipartFile(
                "file", "receipt.pdf", "application/pdf", "content".getBytes());

        ResponseEntity<List<Transaction>> response = controller.extractAndSave(fakePdf, "bt", null);

        assertEquals(401, response.getStatusCode().value());
        verifyNoInteractions(cloudinaryService);
        verifyNoInteractions(extractionPipelineService);
    }
}
