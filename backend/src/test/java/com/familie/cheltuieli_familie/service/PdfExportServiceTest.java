package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.FamilyMember;
import com.familie.cheltuieli_familie.model.User;
import com.familie.cheltuieli_familie.repository.ExpenseRepository;
import com.familie.cheltuieli_familie.repository.ExpenseRepository.ExpenseWithLocationProjection;
import com.familie.cheltuieli_familie.repository.FamilyMemberRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@ActiveProfiles("test")
class PdfExportServiceTest {

    // ── Projection helper ─────────────────────────────────────────────────────
    private record TestProjection(
            Long id, BigDecimal amount, String currency, String description,
            LocalDateTime expenseDate, String category, String person,
            Long locationId, String store, String address,
            String city, String country, Double lat, Double lng
    ) implements ExpenseWithLocationProjection {
        public Long getId()            { return id; }
        public BigDecimal getAmount()  { return amount; }
        public String getCurrency()    { return currency; }
        public String getDescription() { return description; }
        public LocalDateTime getExpenseDate() { return expenseDate; }
        public String getCategory()    { return category; }
        public String getPerson()      { return person; }
        public Long getLocationId()    { return locationId; }
        public String getStore()       { return store; }
        public String getAddress()     { return address; }
        public String getCity()        { return city; }
        public String getCountry()     { return country; }
        public Double getLat()         { return lat; }
        public Double getLng()         { return lng; }
    }

    @Mock private ExpenseRepository      expenseRepository;
    @Mock private FamilyMemberRepository familyMemberRepository;
    @InjectMocks private PdfExportService pdfExportService;

    private User         parentUser;
    private Family       family;
    private FamilyMember familyMember;
    private Authentication parentAuth;

    @BeforeEach
    void setUp() {
        parentUser = new User();
        parentUser.setId(1L);
        parentUser.setEmail("parent@test.com");
        parentUser.setName("Ana Popescu");

        family = new Family();
        family.setId(10L);
        family.setName("Familia Popescu");

        familyMember = new FamilyMember();
        familyMember.setId(1L);
        familyMember.setUser(parentUser);
        familyMember.setFamily(family);

        parentAuth = mock(Authentication.class);
        when(parentAuth.getPrincipal()).thenReturn(parentUser);
        when(parentAuth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_PARENT")));
    }

    // ── Helper: creează un expense în perioada dată ───────────────────────────
    private TestProjection expense(LocalDate date, double amount, String category, String person) {
        return new TestProjection(
                1L, BigDecimal.valueOf(amount), "RON", "Descriere test",
                date.atStartOfDay(), category, person,
                null, null, null, null, null, null, null
        );
    }

    // ── Tests ─────────────────────────────────────────────────────────────────

    @Test
    void generatePdf_returnsByteArray_forParentWithFamily() throws Exception {
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 100.0, "Supermarket", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_returnsPdf_withMultipleExpenses() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to   = LocalDate.now();

        List<ExpenseWithLocationProjection> expenses = List.of(
                expense(LocalDate.now().minusDays(5),  200.0, "Supermarket", "Ana"),
                expense(LocalDate.now().minusDays(10), 150.0, "Transport",   "Mihai"),
                expense(LocalDate.now().minusDays(20), 300.0, "Restaurant",  "Ana")
        );

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null)).thenReturn(expenses);

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
    }

    @Test
    void generatePdf_returnsEmptyPdf_whenNoExpensesInPeriod() throws Exception {
        LocalDate from = LocalDate.of(2020, 1, 1);
        LocalDate to   = LocalDate.of(2020, 1, 31);

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expense(LocalDate.now(), 100.0, "Supermarket", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_usesUserFilter_whenNoFamilyFound() throws Exception {
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());
        when(expenseRepository.findAllByUserFiltered(1L, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 50.0, "Taxi", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        verify(expenseRepository).findAllByUserFiltered(1L, null, null);
    }

    @Test
    void generatePdf_usesChildFilter_forChildRole() throws Exception {
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to   = LocalDate.now();

        Authentication childAuth = mock(Authentication.class);
        when(childAuth.getPrincipal()).thenReturn(parentUser);
        when(childAuth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_CHILD")));

        when(expenseRepository.findAllByUserFiltered(1L, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 30.0, "Cafenea", "Sofia")));

        byte[] pdf = pdfExportService.generatePdf(from, to, childAuth);

        assertNotNull(pdf);
        verify(expenseRepository).findAllByUserFiltered(1L, null, null);
        verify(familyMemberRepository, never()).findByUserId(anyLong());
    }

    @Test
    void generatePdf_returnsEmptyContent_whenAuthIsNull() throws Exception {
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to   = LocalDate.now();

        byte[] pdf = pdfExportService.generatePdf(from, to, null);

        assertNotNull(pdf);
        verify(expenseRepository, never()).findAllByFamilyFiltered(anyLong(), any(), any(), any());
    }

    @Test
    void generatePdf_groupsWeekly_for3MonthPeriod() throws Exception {
        LocalDate from = LocalDate.now().minusDays(89);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(
                        expense(from.plusDays(5),  100.0, "Supermarket", "Ana"),
                        expense(from.plusDays(20), 200.0, "Restaurant",  "Mihai"),
                        expense(from.plusDays(50), 150.0, "Taxi",        "Ana")
                ));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_groupsMonthly_forYearPeriod() throws Exception {
        LocalDate from = LocalDate.now().minusDays(365);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(
                        expense(from.plusDays(10),  500.0, "Supermarket", "Ana"),
                        expense(from.plusDays(40),  300.0, "Restaurant",  "Mihai"),
                        expense(from.plusDays(200), 700.0, "Supermarket", "Ana")
                ));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_handlesNullFieldsGracefully() throws Exception {
        LocalDate from = LocalDate.now().minusDays(6);
        LocalDate to   = LocalDate.now();

        TestProjection expenseWithNulls = new TestProjection(
                2L, BigDecimal.valueOf(75.0), "RON", null,
                LocalDate.now().minusDays(1).atStartOfDay(),
                null, null, null, null, null, null, null, null, null
        );

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expenseWithNulls));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}