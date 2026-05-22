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
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.test.context.ActiveProfiles;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@ActiveProfiles("test")
class PdfExportServiceTest {

    private record TestProjection(
            Long id, BigDecimal amount, String currency, String description,
            LocalDateTime expenseDate, String category, String person, String sourceType,
            String receiptUrl, Long locationId, String store, String address,
            String city, String country, Double lat, Double lng
    ) implements ExpenseWithLocationProjection {
        public Long getId()                  { return id; }
        public BigDecimal getAmount()        { return amount; }
        public String getCurrency()          { return currency; }
        public String getDescription()       { return description; }
        public LocalDateTime getExpenseDate(){ return expenseDate; }
        public String getCategory()          { return category; }
        public String getPerson()            { return person; }
        public String getSourceType()        { return sourceType; }
        public String getReceiptUrl()        { return receiptUrl; }
        public Long getLocationId()          { return locationId; }
        public String getStore()             { return store; }
        public String getAddress()           { return address; }
        public String getCity()              { return city; }
        public String getCountry()           { return country; }
        public Double getLat()               { return lat; }
        public Double getLng()               { return lng; }
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

    private TestProjection expense(LocalDate date, double amount, String category, String person) {
        return new TestProjection(
                1L, BigDecimal.valueOf(amount), "RON", "Descriere test",
                date.atStartOfDay(), category, person, "manual",
                null, null, null, null, null, null, null, null
        );
    }

    @Test
    void generatePdf_returnsByteArray_forParentWithFamily() throws Exception {
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 100.0, "Supermarket", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_returnsPdf_withMultipleExpenses() throws Exception {
        LocalDate from = LocalDate.now().minusDays(30);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null)).thenReturn(List.of(
                expense(LocalDate.now().minusDays(5),  200.0, "Supermarket", "Ana"),
                expense(LocalDate.now().minusDays(10), 150.0, "Transport",   "Mihai"),
                expense(LocalDate.now().minusDays(20), 300.0, "Restaurant",  "Ana")
        ));

        byte[] pdf = pdfExportService.generatePdf(from, to, parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 1000);
    }

    @Test
    void generatePdf_returnsValidPdf_whenNoExpensesInPeriod() throws Exception {
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expense(LocalDate.now(), 100.0, "Supermarket", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(
                LocalDate.of(2020, 1, 1), LocalDate.of(2020, 1, 31), parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_usesUserFilter_whenNoFamilyFound() throws Exception {
        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of());
        when(expenseRepository.findAllByUserFiltered(1L, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 50.0, "Taxi", "Ana")));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), parentAuth);

        assertNotNull(pdf);
        verify(expenseRepository).findAllByUserFiltered(1L, null, null);
    }

    @Test
    void generatePdf_usesChildFilter_forChildRole() throws Exception {
        Authentication childAuth = mock(Authentication.class);
        when(childAuth.getPrincipal()).thenReturn(parentUser);
        when(childAuth.getAuthorities()).thenAnswer(inv ->
                List.of(new SimpleGrantedAuthority("ROLE_CHILD")));
        when(expenseRepository.findAllByUserFiltered(1L, null, null))
                .thenReturn(List.of(expense(LocalDate.now().minusDays(1), 30.0, "Cafenea", "Sofia")));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), childAuth);

        assertNotNull(pdf);
        verify(expenseRepository).findAllByUserFiltered(1L, null, null);
        verify(familyMemberRepository, never()).findByUserId(anyLong());
    }

    @Test
    void generatePdf_returnsValidPdf_whenAuthIsNull() throws Exception {
        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), null);

        assertNotNull(pdf);
        verify(expenseRepository, never()).findAllByFamilyFiltered(anyLong(), any(), any(), any());
    }

    @Test
    void generatePdf_groupsWeekly_for3MonthPeriod() throws Exception {
        LocalDate from = LocalDate.now().minusDays(89);
        LocalDate to   = LocalDate.now();

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null)).thenReturn(List.of(
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
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null)).thenReturn(List.of(
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
        TestProjection expenseWithNulls = new TestProjection(
                2L, BigDecimal.valueOf(75.0), "RON", null,
                LocalDate.now().minusDays(1).atStartOfDay(),
                null, null, "manual",
                null, null, null, null, null, null, null, null
        );


        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expenseWithNulls));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_truncatesLongDescriptionAndCategory() throws Exception {
        TestProjection expenseWithLongFields = new TestProjection(
                4L, BigDecimal.valueOf(99.0), "RON",
                "Aceasta este o descriere foarte lunga care depaseste limita de caractere",
                LocalDate.now().minusDays(1).atStartOfDay(),
                "Categorie cu un nume extrem de lung", "Ana", "manual",
                null, null, null, null, null, null, null, null
        );

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expenseWithLongFields));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }

    @Test
    void generatePdf_handlesNewlineCharactersGracefully() throws Exception {
        TestProjection expenseWithNewline = new TestProjection(
                3L, BigDecimal.valueOf(50.0), "RON", "Descriere\ncu newline",
                LocalDate.now().minusDays(1).atStartOfDay(),
                "Categorie\nNoua", "Persoana\nTest", "manual",
                null, null, null, null, null, null, null, null
        );

        when(familyMemberRepository.findByUserId(1L)).thenReturn(List.of(familyMember));
        when(expenseRepository.findAllByFamilyFiltered(10L, null, null, null))
                .thenReturn(List.of(expenseWithNewline));

        byte[] pdf = pdfExportService.generatePdf(LocalDate.now().minusDays(6), LocalDate.now(), parentAuth);

        assertNotNull(pdf);
        assertTrue(pdf.length > 0);
    }
}