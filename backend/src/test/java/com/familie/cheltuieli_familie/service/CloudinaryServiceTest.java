package com.familie.cheltuieli_familie.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.Uploader;
import com.familie.cheltuieli_familie.exception.ExternalServiceException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.io.File;
import java.io.IOException;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CloudinaryServiceTest {

    @Mock
    private Cloudinary cloudinary;

    @Mock
    private Uploader uploader;

    @InjectMocks
    private CloudinaryService cloudinaryService;

    @BeforeEach
    void setUp() {
        lenient().when(cloudinary.uploader()).thenReturn(uploader);
    }

    @Test
    void testUploadFileSuccess() throws IOException {
        File file = mock(File.class);
        when(file.length()).thenReturn(1024L);

        Map<String, Object> uploadResult = Map.of("secure_url", "https://cloudinary.com/test-url");
        when(uploader.upload(any(File.class), anyMap())).thenReturn(uploadResult);

        String result = cloudinaryService.uploadFile(file, "receipts/2026-05", "receipt.jpg");

        assertEquals("https://cloudinary.com/test-url", result);
        verify(uploader, times(1)).upload(any(File.class), anyMap());
    }

    @Test
    void testUploadFileIOException() throws IOException {
        File file = mock(File.class);
        when(file.length()).thenReturn(1024L);

        when(uploader.upload(any(File.class), anyMap())).thenThrow(new IOException("Network error"));

        ExternalServiceException exception = assertThrows(ExternalServiceException.class,
                () -> cloudinaryService.uploadFile(file, "receipts/2026-05", "receipt.jpg"));

        assertTrue(exception.getMessage().contains("Failed to upload file to Cloudinary"));
        assertTrue(exception.getCause() instanceof IOException);
    }

    @Test
    void testDeleteFileSuccess() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenReturn(Map.of("result", "ok"));

        cloudinaryService.deleteFile("receipts/2026-05/receipt_jpg");

        verify(uploader, times(1)).destroy(eq("receipts/2026-05/receipt_jpg"), anyMap());
    }

    @Test
    void testDeleteFileIOException() throws IOException {
        when(uploader.destroy(anyString(), anyMap())).thenThrow(new IOException("Network error"));

        // Should not throw; method catches IOException internally
        assertDoesNotThrow(() -> cloudinaryService.deleteFile("receipts/2026-05/receipt_jpg"));

        verify(uploader, times(1)).destroy(eq("receipts/2026-05/receipt_jpg"), anyMap());
    }

    @Test
    void testUploadFileUnexpectedException() throws IOException {
        File file = mock(File.class);
        when(file.length()).thenReturn(1024L);

        when(uploader.upload(any(File.class), anyMap())).thenThrow(new RuntimeException("Unexpected error"));

        ExternalServiceException exception = assertThrows(ExternalServiceException.class,
                () -> cloudinaryService.uploadFile(file, "receipts/2026-05", "receipt.jpg"));

        assertTrue(exception.getMessage().contains("Unexpected error during Cloudinary upload"));
        assertTrue(exception.getCause() instanceof RuntimeException);
    }

    @Test
    void testSanitizePublicId() {
        String result = ReflectionTestUtils.invokeMethod(cloudinaryService, "sanitizePublicId", "my-file@name#1.jpg");

        assertEquals("my-file_name_1.jpg", result);
    }

    @Test
    void testSanitizePublicIdMultipleSpecialChars() {
        String result = ReflectionTestUtils.invokeMethod(cloudinaryService, "sanitizePublicId", "file@name#test");

        assertEquals("file_name_test", result);
    }

    @Test
    void testSanitizePublicIdLongName() {
        String longName = "a".repeat(150);
        String result = ReflectionTestUtils.invokeMethod(cloudinaryService, "sanitizePublicId", longName);

        assertEquals(100, result.length());
    }
}
