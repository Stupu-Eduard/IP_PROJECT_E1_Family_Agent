package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.OcrResponseDTO;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.ReceiptParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class OcrControllerTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private ReceiptParser receiptParser;

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private OcrController ocrController;

    @Test
    void testProcessReceiptUnauthorized() {
        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class, () ->
                ocrController.processReceipt(file, null));

        assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatusCode());
    }

    @Test
    void testProcessReceiptImageSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Kaufland");
        receipt.setTotalAmount(new BigDecimal("150.50"));
        receipt.setDate("15/03/2024");
        receipt.setCategory("Mâncare");
        receipt.setItems(List.of());

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Mâncare");
        when(categoryRepository.findByName("Mâncare")).thenReturn(Optional.of(category));

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("150.50"), response.getBody().amount());
        assertEquals("Mâncare", response.getBody().category());
        assertEquals("Kaufland", response.getBody().locationName());
        assertNotNull(response.getBody().items());
    }

    @Test
    void testProcessReceiptPdfSuccess() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.pdf", "application/pdf", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromPdf(any())).thenReturn("PDF OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("OMV");
        receipt.setTotalAmount(new BigDecimal("200.00"));
        receipt.setDate("20.04.2024");
        receipt.setCategory("Transport");
        receipt.setItems(List.of());

        when(receiptParser.parseReceipt("PDF OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Transport");
        when(categoryRepository.findByName("Transport")).thenReturn(Optional.of(category));

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("200.00"), response.getBody().amount());
        assertEquals("Transport", response.getBody().category());
    }

    @Test
    void testProcessReceiptParsingFailed() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");
        when(receiptParser.parseReceipt("OCR text")).thenReturn(null);

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertNull(response.getBody().amount());
    }

    @Test
    void testProcessReceiptWithItems() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Lidl");
        receipt.setTotalAmount(new BigDecimal("50.00"));
        receipt.setDate("01.05.2024");
        receipt.setCategory("Mâncare");

        ReceiptParser.ReceiptItem item = new ReceiptParser.ReceiptItem();
        item.setName("Pâine");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("5.00"));
        receipt.setItems(List.of(item));

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Mâncare");
        when(categoryRepository.findByName("Mâncare")).thenReturn(Optional.of(category));

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(1, response.getBody().items().size());
        assertEquals("Pâine", response.getBody().items().get(0).name());
        assertEquals(new BigDecimal("2"), response.getBody().items().get(0).quantity());
        assertEquals(new BigDecimal("5.00"), response.getBody().items().get(0).unitPrice());
    }

    @Test
    void testProcessReceiptCategoryNotFound_fallsBackToFirst() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.setStoreName("Necunoscut");
        receipt.setTotalAmount(new BigDecimal("10.00"));
        receipt.setDate(null);
        receipt.setCategory("Diverse");
        receipt.setItems(List.of());

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category fallback = new Category();
        fallback.setName("Mâncare");
        when(categoryRepository.findByName("Diverse")).thenReturn(Optional.empty());
        when(categoryRepository.findAll()).thenReturn(List.of(fallback));

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals("Mâncare", response.getBody().category());
    }
}
