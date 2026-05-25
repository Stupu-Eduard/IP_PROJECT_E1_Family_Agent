package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.CreateExpenseRequest;
import com.familie.cheltuieli_familie.dto.ExpenseListDto;
import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.mapper.ExpenseMapper;
import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.CategoryRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository.ExpenseWithLocationProjection;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ExpenseControllerTest {

    @Mock
    private ExpenseRepository expenseRepository;

    @Mock
    private CategoryRepository categoryRepository;

    @Mock
    private FamilyMemberRepository familyMemberRepository;

    @Mock
    private LocationRepository locationRepository;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private ExpenseMapper expenseMapper;

    @InjectMocks
    private ExpenseController expenseController;

    // ── helpers ───────────────────────────────────────────────────────────────

    private User createUser(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setName("Test User");
        return user;
    }

    private User mockUser(Long id) {
        User user = mock(User.class);
        when(user.getId()).thenReturn(id);
        return user;
    }

    private Authentication createAuth(User user) {
        return new UsernamePasswordAuthenticationToken(user, null, Collections.emptyList());
    }

    private Authentication parentAuth(User user) {
        return new UsernamePasswordAuthenticationToken(
                user, null, List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
    }

    private FamilyMember mockMembership(Long familyId) {
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(familyId);
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getFamily()).thenReturn(family);
        when(fm.getRole()).thenReturn("Parent");
        return fm;
    }

    private ExpenseListDto createExpenseListDto() {
        return new ExpenseListDto(
                1L, new BigDecimal("100"), "RON", "Test",
                LocalDateTime.now(), "Food", "Test User",
                null, "MANUAL", null
        );
    }

    private ExpenseWithLocationProjection mockProjection(
            Long id, BigDecimal amount, String currency, String desc,
            LocalDateTime date, String category, String familyName,
            String sourceType, String receiptUrl,
            Long locationId, String store, String address,
            String city, String country, Double lat, Double lng) {

        ExpenseWithLocationProjection p = mock(ExpenseWithLocationProjection.class);
        when(p.getId()).thenReturn(id);
        when(p.getAmount()).thenReturn(amount);
        when(p.getCurrency()).thenReturn(currency);
        when(p.getDescription()).thenReturn(desc);
        when(p.getExpenseDate()).thenReturn(date);
        when(p.getCategory()).thenReturn(category);
        when(p.getPerson()).thenReturn(familyName);
        when(p.getSourceType()).thenReturn(sourceType);
        when(p.getReceiptUrl()).thenReturn(receiptUrl);
        when(p.getLocationId()).thenReturn(locationId);
        when(p.getStore()).thenReturn(store);
        when(p.getAddress()).thenReturn(address);
        when(p.getCity()).thenReturn(city);
        when(p.getCountry()).thenReturn(country);
        when(p.getLat()).thenReturn(lat);
        when(p.getLng()).thenReturn(lng);
        return p;
    }

    // ── list ─────────────────────────────────────────────────────────────────

    @Test
    void list_shouldReturnEmptyListWhenNotAuthenticated() {
        List<ExpenseListDto> result = expenseController.list(null, null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void list_withoutLocation_returnsNullLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(2L);
        FamilyMember membership5 = mockMembership(5L);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of(membership5));

        ExpenseWithLocationProjection row = mockProjection(
                1L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null, null, "manual", null, null, null, null, null, null, null, null);

        when(expenseRepository.findAllByFamilyFiltered(eq(5L), isNull(), isNull(), isNull()))
                .thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, parentAuth(user));

        assertEquals(1, result.size());
        assertNull(result.getFirst().location());
    }

    @Test
    void list_childAuth_usesUserFilteredQuery() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CHILD")));

        ExpenseWithLocationProjection row = mockProjection(
                5L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                null, null, "manual", null, null, null, null, null, null, null, null);
        when(expenseRepository.findAllByUserFiltered(eq(3L), isNull(), isNull())).thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, auth);

        assertEquals(1, result.size());
        assertEquals(5L, result.getFirst().id());
    }

    @Test
    void list_parentWithoutFamily_fallsBackToUserFiltered() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of());

        ExpenseWithLocationProjection row = mockProjection(
                7L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 3, 1, 0, 0),
                null, null, "manual", null, null, null, null, null, null, null, null);
        when(expenseRepository.findAllByUserFiltered(eq(4L), isNull(), isNull())).thenReturn(List.of(row));

        List<ExpenseListDto> result = controller.list(null, null, null, auth);

        assertEquals(1, result.size());
        assertEquals(7L, result.getFirst().id());
    }

    @Test
    void list_nullAuth_returnsEmptyList() {
        ExpenseController controller = new ExpenseController(
                mock(ExpenseRepository.class), mock(CategoryRepository.class),
                mock(FamilyMemberRepository.class), mock(LocationRepository.class),
                mock(ApplicationEventPublisher.class), new ExpenseMapper());

        List<ExpenseListDto> result = controller.list(null, null, null, null);
        assertTrue(result.isEmpty());
    }

    @Test
    void list_withCoParentRole_worksLikeParent() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(auth.getAuthorities()).thenAnswer(i -> List.of(new SimpleGrantedAuthority("ROLE_CO-PARENT")));

        FamilyMember membership = mockMembership(100L);
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(membership));
        when(expenseRepository.findAllByFamilyFiltered(eq(100L), any(), any(), any())).thenReturn(List.of());

        controller.list(null, null, null, auth);
        verify(expenseRepository).findAllByFamilyFiltered(eq(100L), any(), any(), any());
    }

    @Test
    void list_principalNotUser_returnsEmpty() {
        ExpenseController controller = new ExpenseController(
                mock(ExpenseRepository.class), mock(CategoryRepository.class),
                mock(FamilyMemberRepository.class), mock(LocationRepository.class),
                mock(ApplicationEventPublisher.class), new ExpenseMapper());

        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn("NotAUser");

        List<ExpenseListDto> result = controller.list(null, null, null, auth);
        assertTrue(result.isEmpty());
    }

    @Test
    void list_shouldReturnUserExpenses() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(Collections.emptyList());
        when(expenseRepository.findAllByUserFiltered(eq(1L), any(), any()))
                .thenReturn(Collections.emptyList());

        List<ExpenseListDto> result = expenseController.list(null, null, null, auth);
        assertNotNull(result);
    }

    // ── getById ──────────────────────────────────────────────────────────────

    @Test
    void getById_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class),
                mock(FamilyMemberRepository.class), mock(LocationRepository.class),
                mock(ApplicationEventPublisher.class), new ExpenseMapper());

        when(expenseRepository.findOneWithLocation(99L)).thenReturn(null);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.getById(99L));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void getById_whenFound_mapsToDto() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class),
                mock(FamilyMemberRepository.class), mock(LocationRepository.class),
                mock(ApplicationEventPublisher.class), new ExpenseMapper());

        ExpenseWithLocationProjection row = mockProjection(
                2L, BigDecimal.TEN, "RON", "desc",
                LocalDateTime.of(2026, 2, 2, 12, 0),
                "Utilities", "Family", "manual",
                null, null, null, null, null, null, null, null);

        when(expenseRepository.findOneWithLocation(2L)).thenReturn(row);

        ExpenseListDto dto = controller.getById(2L);

        assertEquals(2L, dto.id());
        assertEquals(BigDecimal.TEN, dto.amount());
        assertEquals("Utilities", dto.category());
        assertNull(dto.location());
    }

    @Test
    void getById_shouldReturnExpense() {
        ExpenseWithLocationProjection projection = mock(ExpenseWithLocationProjection.class);
        when(expenseRepository.findOneWithLocation(1L)).thenReturn(projection);
        when(expenseMapper.toDto(any())).thenReturn(createExpenseListDto());

        ExpenseListDto result = expenseController.getById(1L);
        assertNotNull(result);
    }

    @Test
    void getById_shouldThrowWhenNotFound() {
        when(expenseRepository.findOneWithLocation(999L)).thenReturn(null);

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.getById(999L));
        assertEquals(HttpStatus.NOT_FOUND, exception.getStatusCode());
    }

    // ── create ───────────────────────────────────────────────────────────────

    @Test
    void create_withLocation_savesExpenseAndLocation() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                locationRepository, mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());

        Location savedLocation = mock(Location.class);
        when(locationRepository.save(any(Location.class))).thenReturn(savedLocation);

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(10L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        ExpenseWithLocationProjection row = mockProjection(
                10L, BigDecimal.valueOf(50), "RON", "lunch",
                LocalDateTime.of(2026, 5, 1, 12, 0),
                "Food", "Alex", "manual", null,
                1L, "Restaurant", "Main St", "Cluj", "RO", 46.77, 23.59);
        when(expenseRepository.findOneWithLocation(10L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(50));
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 5, 1));
        req.setStoreName("Restaurant");
        req.setCity("Cluj");

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(10L, result.id());
        assertEquals("Food", result.category());
        assertNotNull(result.location());
        verify(locationRepository).save(any(Location.class));
    }

    @Test
    void create_withoutLocation_skipsLocationSave() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                locationRepository, mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(2L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Transport")).thenReturn(Optional.of(category));
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(11L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        ExpenseWithLocationProjection row = mockProjection(
                11L, BigDecimal.valueOf(20), "RON", null,
                LocalDateTime.of(2026, 5, 2, 8, 0),
                "Transport", "Alex", "manual", null,
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(11L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(20));
        req.setCategoryName("Transport");
        req.setDate(LocalDate.of(2026, 5, 2));

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(11L, result.id());
        assertNull(result.location());
        verify(locationRepository, never()).save(any(Location.class));
    }

    @Test
    void create_categoryNotFound_throwsBadRequest() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, mock(FamilyMemberRepository.class),
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(categoryRepository.findByName("Inexistent")).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(10));
        req.setCategoryName("Inexistent");
        req.setDate(LocalDate.of(2026, 5, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.create(req, auth));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void create_withFamily_setsExpenseFamily() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        Family family = mock(Family.class);
        FamilyMember fm = mock(FamilyMember.class);
        when(fm.getFamily()).thenReturn(family);
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of(fm));

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(12L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        ExpenseWithLocationProjection row = mockProjection(
                12L, BigDecimal.valueOf(30), "RON", null,
                LocalDateTime.of(2026, 5, 3, 9, 0),
                "Food", "Alex", "manual", null,
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(12L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(30));
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 5, 3));

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(12L, result.id());
        verify(expenseRepository).save(any(Expense.class));
    }

    @Test
    void create_withReceiptUrl_setsOcrSourceType() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                locationRepository, mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(5L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        Expense savedExpense = mock(Expense.class);
        when(savedExpense.getId()).thenReturn(20L);
        when(expenseRepository.save(any(Expense.class))).thenReturn(savedExpense);

        ExpenseWithLocationProjection row = mockProjection(
                20L, BigDecimal.valueOf(80), "RON", null,
                LocalDateTime.of(2026, 5, 5, 10, 0),
                "Food", "Alex", "OCR",
                "https://cloudinary.com/receipt.jpg",
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(20L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.valueOf(80));
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 5, 5));
        req.setReceiptUrl("https://cloudinary.com/receipt.jpg");

        ExpenseListDto result = controller.create(req, auth);

        assertEquals(20L, result.id());
        assertEquals("OCR", result.sourceType());
    }

    @Test
    void create_shouldCreateExpense() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setAmount(new BigDecimal("100"));
        request.setDescription("Test");
        request.setDate(LocalDate.now());
        request.setCategoryName("Food");

        Category category = new Category();
        category.setName("Food");

        Expense expense = new Expense();
        expense.setId(1L);

        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));
        when(expenseRepository.save(any(Expense.class))).thenReturn(expense);
        when(expenseRepository.findOneWithLocation(1L)).thenReturn(null);
        when(expenseMapper.toDto(any())).thenReturn(createExpenseListDto());

        ExpenseListDto result = expenseController.create(request, auth);
        assertNotNull(result);
    }

    @Test
    void create_shouldThrowWhenCategoryNotFound() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        CreateExpenseRequest request = new CreateExpenseRequest();
        request.setCategoryName("NonExistent");

        when(categoryRepository.findByName("NonExistent")).thenReturn(Optional.empty());

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.create(request, auth));
        assertEquals(HttpStatus.BAD_REQUEST, exception.getStatusCode());
    }

    // ── update ───────────────────────────────────────────────────────────────

    @Test
    void update_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class),
                mock(FamilyMemberRepository.class), mock(LocationRepository.class),
                mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(99L, req, auth));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void update_child_nonManualExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("ocr");
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(1L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_anotherPersonExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        User otherUser = mockUser(99L);
        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(otherUser);
        when(expenseRepository.findById(2L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(2L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_nullOwner_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(null);
        when(expenseRepository.findById(3L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(3L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_child_ownManualExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of(childMember));

        Expense expense = mock(Expense.class);
        when(expense.getSourceType()).thenReturn("manual");
        when(expense.getUser()).thenReturn(user);
        when(expense.getId()).thenReturn(4L);
        when(expenseRepository.findById(4L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        ExpenseWithLocationProjection row = mockProjection(
                4L, BigDecimal.ONE, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "Food", null, "manual", null,
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(4L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(4L, req, auth);
        assertEquals(4L, result.id());
    }

    @Test
    void update_parent_ownExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(5L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expense.getId()).thenReturn(5L);
        when(expenseRepository.findById(5L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        ExpenseWithLocationProjection row = mockProjection(
                5L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "Food", null, "manual", null,
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(5L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(5L, req, auth);
        assertEquals(5L, result.id());
    }

    @Test
    void update_parent_familyExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(6L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(6L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(20L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expense.getId()).thenReturn(6L);
        when(expenseRepository.findById(6L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(familyMemberRepository.existsByFamilyIdAndUserId(20L, 6L)).thenReturn(true);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        ExpenseWithLocationProjection row = mockProjection(
                6L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "Food", null, "manual", null,
                null, null, null, null, null, null, null);
        when(expenseRepository.findOneWithLocation(6L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ExpenseListDto result = controller.update(6L, req, auth);
        assertEquals(6L, result.id());
    }

    @Test
    void update_parent_noAccess_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(7L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(7L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(30L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(7L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(30L, 7L)).thenReturn(false);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(7L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_parent_noFamilyOnExpense_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(8L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(8L)).thenReturn(List.of());

        User otherUser = mockUser(99L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(null);
        when(expenseRepository.findById(8L)).thenReturn(Optional.of(expense));

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(8L, req, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void update_categoryNotFound_throwsBadRequest() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(9L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(9L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expenseRepository.findById(9L)).thenReturn(Optional.of(expense));
        when(categoryRepository.findByName("Unknown")).thenReturn(Optional.empty());

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.ONE);
        req.setCategoryName("Unknown");
        req.setDate(LocalDate.of(2026, 1, 1));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.update(9L, req, auth));
        assertEquals(HttpStatus.BAD_REQUEST, ex.getStatusCode());
    }

    @Test
    void update_withExistingLocation_updatesInPlace() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        CategoryRepository categoryRepository = mock(CategoryRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        LocationRepository locationRepository = mock(LocationRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, categoryRepository, familyMemberRepository,
                locationRepository, mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(10L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(10L)).thenReturn(List.of());

        Location existingLocation = mock(Location.class);
        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expense.getLocation()).thenReturn(existingLocation);
        when(expense.getId()).thenReturn(10L);
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));
        when(expenseRepository.save(expense)).thenReturn(expense);
        when(locationRepository.save(existingLocation)).thenReturn(existingLocation);

        Category category = mock(Category.class);
        when(categoryRepository.findByName("Food")).thenReturn(Optional.of(category));

        ExpenseWithLocationProjection row = mockProjection(
                10L, BigDecimal.TEN, "RON", null,
                LocalDateTime.of(2026, 1, 1, 0, 0),
                "Food", null, "manual", null,
                1L, "Updated Store", null, "Cluj", null, null, null);
        when(expenseRepository.findOneWithLocation(10L)).thenReturn(row);

        CreateExpenseRequest req = new CreateExpenseRequest();
        req.setAmount(BigDecimal.TEN);
        req.setCategoryName("Food");
        req.setDate(LocalDate.of(2026, 1, 1));
        req.setStoreName("Updated Store");
        req.setCity("Cluj");

        ExpenseListDto result = controller.update(10L, req, auth);
        assertEquals(10L, result.id());
        verify(locationRepository).save(existingLocation);
    }

    // ── delete ───────────────────────────────────────────────────────────────

    @Test
    void delete_child_throwsForbidden() {
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                mock(ExpenseRepository.class), mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(1L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);

        FamilyMember childMember = mock(FamilyMember.class);
        when(childMember.getRole()).thenReturn("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(childMember));

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(1L, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void delete_whenNotFound_throwsNotFound() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(2L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(2L)).thenReturn(List.of());
        when(expenseRepository.findById(99L)).thenReturn(Optional.empty());

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(99L, auth));
        assertEquals(HttpStatus.NOT_FOUND, ex.getStatusCode());
    }

    @Test
    void delete_parent_ownExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(3L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(3L)).thenReturn(List.of());

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(user);
        when(expenseRepository.findById(10L)).thenReturn(Optional.of(expense));

        assertDoesNotThrow(() -> controller.delete(10L, auth));
        verify(expenseRepository).deleteById(10L);
    }

    @Test
    void delete_parent_familyExpense_succeeds() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(4L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(4L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(40L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(11L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(40L, 4L)).thenReturn(true);

        assertDoesNotThrow(() -> controller.delete(11L, auth));
        verify(expenseRepository).deleteById(11L);
    }

    @Test
    void delete_parent_noAccess_throwsForbidden() {
        ExpenseRepository expenseRepository = mock(ExpenseRepository.class);
        FamilyMemberRepository familyMemberRepository = mock(FamilyMemberRepository.class);
        ExpenseController controller = new ExpenseController(
                expenseRepository, mock(CategoryRepository.class), familyMemberRepository,
                mock(LocationRepository.class), mock(ApplicationEventPublisher.class), new ExpenseMapper());

        User user = mockUser(5L);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(user);
        when(familyMemberRepository.findByUserId(5L)).thenReturn(List.of());

        User otherUser = mockUser(99L);
        Family family = mock(Family.class);
        when(family.getId()).thenReturn(50L);

        Expense expense = mock(Expense.class);
        when(expense.getUser()).thenReturn(otherUser);
        when(expense.getFamily()).thenReturn(family);
        when(expenseRepository.findById(12L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(50L, 5L)).thenReturn(false);

        ResponseStatusException ex = assertThrows(ResponseStatusException.class,
                () -> controller.delete(12L, auth));
        assertEquals(HttpStatus.FORBIDDEN, ex.getStatusCode());
    }

    @Test
    void delete_shouldThrowWhenChildUser() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        FamilyMember member = new FamilyMember();
        member.setRole("Child");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        ResponseStatusException exception = assertThrows(ResponseStatusException.class,
                () -> expenseController.delete(1L, auth));
        assertEquals(HttpStatus.FORBIDDEN, exception.getStatusCode());
    }

    @Test
    void delete_shouldDeleteWhenParentUser() {
        User user = createUser(1L, "test@test.com");
        Authentication auth = createAuth(user);

        FamilyMember member = new FamilyMember();
        member.setRole("Parent");
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(member));

        Expense expense = new Expense();
        expense.setId(1L);
        expense.setUser(user);
        when(expenseRepository.findById(1L)).thenReturn(Optional.of(expense));
        when(familyMemberRepository.existsByFamilyIdAndUserId(any(), any())).thenReturn(true);

        assertDoesNotThrow(() -> expenseController.delete(1L, auth));
        verify(expenseRepository).deleteById(1L);
    }
}