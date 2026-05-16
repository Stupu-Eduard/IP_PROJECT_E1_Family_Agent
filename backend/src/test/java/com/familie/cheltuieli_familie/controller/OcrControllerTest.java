package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.OcrResponseDTO;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseItemRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import com.familie.cheltuieli_familie.service.CloudinaryService;
import com.familie.cheltuieli_familie.service.OcrService;
import com.familie.cheltuieli_familie.service.ReceiptParser;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OcrControllerTest {

    @Mock
    private OcrService ocrService;

    @Mock
    private ReceiptParser receiptParser;

    @Mock
    private CloudinaryService cloudinaryService;

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private ExpenseItemRepository expenseItemRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

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
        receipt.storeName = "Kaufland";
        receipt.totalAmount = new BigDecimal("150.50");
        receipt.date = "15/03/2024";
        receipt.category = "Mâncare";
        receipt.items = List.of();

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Mâncare");
        when(categoryRepository.findByName("Mâncare")).thenReturn(Optional.of(category));

        Location location = new Location();
        location.setStore("Kaufland");
        when(locationRepository.findAll()).thenReturn(List.of(location));

        when(cloudinaryService.uploadFile(any(), anyString(), anyString())).thenReturn("https://cloudinary.com/test");

        Expense savedExpense = new Expense();
        savedExpense.setId(100L);
        savedExpense.setAmount(new BigDecimal("150.50"));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("150.50"), response.getBody().amount());
        assertEquals("Mâncare", response.getBody().category());
        assertEquals("Kaufland", response.getBody().locationName());
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
        receipt.storeName = "OMV";
        receipt.totalAmount = new BigDecimal("200.00");
        receipt.date = "20.04.2024";
        receipt.category = "Transport";
        receipt.items = List.of();

        when(receiptParser.parseReceipt("PDF OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Transport");
        when(categoryRepository.findByName("Transport")).thenReturn(Optional.of(category));

        when(locationRepository.findAll()).thenReturn(List.of());

        Location newLoc = new Location();
        newLoc.setStore("OMV");
        when(locationRepository.save(any(Location.class))).thenReturn(newLoc);

        when(cloudinaryService.uploadFile(any(), anyString(), anyString())).thenReturn("https://cloudinary.com/test");

        Expense savedExpense = new Expense();
        savedExpense.setId(101L);
        savedExpense.setAmount(new BigDecimal("200.00"));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("200.00"), response.getBody().amount());
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
        receipt.storeName = "Lidl";
        receipt.totalAmount = new BigDecimal("50.00");
        receipt.date = "01.05.2024";
        receipt.category = "Mâncare";

        ReceiptParser.ReceiptItem item = new ReceiptParser.ReceiptItem();
        item.name = "Pâine";
        item.quantity = new BigDecimal("2");
        item.unitPrice = new BigDecimal("5.00");
        receipt.items = List.of(item);

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Mâncare");
        when(categoryRepository.findByName("Mâncare")).thenReturn(Optional.of(category));

        when(locationRepository.findAll()).thenReturn(List.of());

        Location newLoc = new Location();
        newLoc.setStore("Lidl");
        when(locationRepository.save(any(Location.class))).thenReturn(newLoc);

        when(cloudinaryService.uploadFile(any(), anyString(), anyString())).thenReturn("https://cloudinary.com/test");

        Expense savedExpense = new Expense();
        savedExpense.setId(102L);
        savedExpense.setAmount(new BigDecimal("50.00"));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        when(expenseItemRepository.save(any())).thenAnswer(i -> i.getArgument(0));
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(expenseItemRepository, times(1)).save(any());
    }

    @Test
    void testProcessReceiptCloudinaryFailure() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.storeName = "Test";
        receipt.totalAmount = new BigDecimal("10.00");
        receipt.date = null;
        receipt.category = "Diverse";
        receipt.items = List.of();

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Diverse");
        when(categoryRepository.findByName("Diverse")).thenReturn(Optional.of(category));

        when(locationRepository.findAll()).thenReturn(List.of());

        when(cloudinaryService.uploadFile(any(), anyString(), anyString())).thenThrow(new RuntimeException("Cloudinary down"));

        Expense savedExpense = new Expense();
        savedExpense.setId(103L);
        savedExpense.setAmount(new BigDecimal("10.00"));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        assertNotNull(response.getBody());
        assertEquals(new BigDecimal("10.00"), response.getBody().amount());
    }

    @Test
    void testProcessReceiptWithFamilyMember() throws Exception {
        User user = new User();
        user.setId(1L);
        user.setName("Test User");

        MockMultipartFile file = new MockMultipartFile("file", "test.jpg", "image/jpeg", "test".getBytes());
        UsernamePasswordAuthenticationToken auth = new UsernamePasswordAuthenticationToken(user, null);

        when(ocrService.extractTextFromImage(any())).thenReturn("OCR text");

        ReceiptParser.ParsedReceipt receipt = new ReceiptParser.ParsedReceipt();
        receipt.storeName = "Kaufland";
        receipt.totalAmount = new BigDecimal("75.00");
        receipt.date = "10/06/2024";
        receipt.category = "Mâncare";
        receipt.items = List.of();

        when(receiptParser.parseReceipt("OCR text")).thenReturn(receipt);

        Category category = new Category();
        category.setName("Mâncare");
        when(categoryRepository.findByName("Mâncare")).thenReturn(Optional.of(category));

        when(locationRepository.findAll()).thenReturn(List.of());

        Location newLoc = new Location();
        newLoc.setStore("Kaufland");
        when(locationRepository.save(any(Location.class))).thenReturn(newLoc);

        when(cloudinaryService.uploadFile(any(), anyString(), anyString())).thenReturn("https://cloudinary.com/test");

        Expense savedExpense = new Expense();
        savedExpense.setId(104L);
        savedExpense.setAmount(new BigDecimal("75.00"));
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        FamilyMember fm = new FamilyMember();
        com.familie.cheltuieli_familie.model.Family family = new com.familie.cheltuieli_familie.model.Family();
        family.setId(10L);
        fm.setFamily(family);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(fm));

        ResponseEntity<OcrResponseDTO> response = ocrController.processReceipt(file, auth);

        assertEquals(HttpStatus.OK, response.getStatusCode());
        verify(expenseRepository).save(argThat(e -> e.getFamily() != null && e.getFamily().getId().equals(10L)));
    }
}
