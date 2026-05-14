package com.familie.cheltuieli_familie.repository;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
@Sql(scripts = {
        "/db/migration/V31__seed_user_family_fmembers_cat.sql",
        "/db/migration/V32__populate_expenses.sql",
        "/db/migration/V40__populate_budgets.sql"
})
public class DataConsistencyIntegrationTest {//

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    @DisplayName("Verify that budgets were populated by V40")
    void testBudgetsPopulated() {
        Integer count = jdbcTemplate.queryForObject("SELECT count(*) FROM budgets", Integer.class);
        assertTrue(count >= 3, "Ar trebui sa existe cel putin 3 bugete populate de Stefana");
    }

    @Test
    @DisplayName("Consistency Check: No orphan expenses in V32 seed data (IDs 1001-1025)")
    void testNoOrphanExpenses() {
        Integer orphans = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM expenses WHERE (user_id IS NULL OR location_id IS NULL) AND id BETWEEN 1001 AND 1025",
                Integer.class);
        assertEquals(0, orphans, "Exista cheltuieli fara utilizator sau locatie!");
    }

    @Test
    @DisplayName("Consistency Check: Family members linkage")
    void testFamilyMembersLinkage() {
        Integer popescuMembers = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM family_members fm " +
                        "JOIN families f ON fm.family_id = f.id " +
                        "WHERE f.name = 'Familia Popescu'", Integer.class);
        assertEquals(3, popescuMembers, "Familia Popescu ar trebui sa aiba 3 membri conform migratiei Adelei");
    }

    @Test
    @DisplayName("Consistency Check: Expense items total vs Expense amount")
    void testExpenseItemsSum() {
        Double totalItems = jdbcTemplate.queryForObject(
                "SELECT sum(amount) FROM expense_items WHERE expense_id = 1001", Double.class);
        assertEquals(185.00, totalItems, 0.01, "Suma itemilor pentru cheltuiala 1001 nu corespunde cu totalul de 185.00");
    }

    @Test
    @DisplayName("Verify Category Hierarchy")
    void testCategoryHierarchy() {
        Integer subCategories = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM categories WHERE parent_id IS NOT NULL", Integer.class);
        assertTrue(subCategories > 0, "Ierarhia de categorii nu a fost creata corect (lipsesc subcategorii)");
    }
}