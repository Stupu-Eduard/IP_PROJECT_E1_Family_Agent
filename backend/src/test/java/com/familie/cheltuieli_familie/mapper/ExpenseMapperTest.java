package com.familie.cheltuieli_familie.mapper;

import com.familie.cheltuieli_familie.model.Category;
import com.familie.cheltuieli_familie.model.Expense;
import com.familie.cheltuieli_familie.model.ExpenseEntity;
import com.familie.cheltuieli_familie.model.Family;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.model.User;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

class ExpenseMapperTest {

    private final ExpenseMapper mapper = new ExpenseMapper();

    @Test
    void testToExpenseEntityWithFullData() {
        Category category = new Category();
        category.setId(1L);
        category.setName("Food");

        Location location = new Location();
        location.setId(2L);
        location.setStore("Kaufland");

        User user = new User();
        user.setId(3L);
        user.setName("Ion");

        Family family = new Family();
        family.setId(4L);
        family.setName("Familia Popescu");

        Expense expense = new Expense();
        expense.setId(10L);
        expense.setAmount(new BigDecimal("150.00"));
        expense.setDescription("Groceries");
        expense.setExpenseDate(LocalDateTime.of(2024, 6, 15, 10, 30));
        expense.setCategory(category);
        expense.setLocation(location);
        expense.setUser(user);
        expense.setFamily(family);
        expense.setRawInput("Bought groceries at Kaufland");
        expense.setSourceType("manual");
        expense.setCreatedAt(LocalDateTime.of(2024, 6, 15, 10, 30));

        ExpenseEntity entity = mapper.toExpenseEntity(expense);

        assertEquals(10L, entity.getId());
        assertEquals(new BigDecimal("150.00"), entity.getAmount());
        assertEquals("Food", entity.getCategory());
        assertEquals("Kaufland", entity.getLocation());
        assertEquals("Ion", entity.getPerson());
        assertEquals("Bought groceries at Kaufland", entity.getRawInput());
        assertEquals(4L, entity.getFamilyId());
        assertEquals(3L, entity.getUserId());
        assertNotNull(entity.getCreatedAt());
        assertEquals(expense.getExpenseDate().toLocalDate(), entity.getDate());
    }

    @Test
    void testToExpenseEntityWithNulls() {
        Expense expense = new Expense();
        expense.setId(20L);
        expense.setAmount(new BigDecimal("50.00"));
        expense.setDescription("Transport");
        expense.setExpenseDate(LocalDateTime.of(2024, 7, 1, 12, 0));
        expense.setCategory(null);
        expense.setLocation(null);
        expense.setUser(null);
        expense.setFamily(null);
        expense.setRawInput(null);
        expense.setSourceType("ocr");
        expense.setCreatedAt(null);

        ExpenseEntity entity = mapper.toExpenseEntity(expense);

        assertEquals(20L, entity.getId());
        assertEquals(new BigDecimal("50.00"), entity.getAmount());
        assertNull(entity.getCategory());
        assertNull(entity.getLocation());
        assertNull(entity.getPerson());
        assertNull(entity.getFamilyId());
        assertNull(entity.getUserId());
        assertNotNull(entity.getRawInput());
        assertTrue(entity.getRawInput().contains("Transport"));
        assertTrue(entity.getRawInput().contains("ocr"));
        assertNotNull(entity.getCreatedAt());
        assertEquals(expense.getExpenseDate().toLocalDate(), entity.getDate());
    }

    @Test
    void testToExpenseEntityMapsFamilyIdAndUserId() {
        Family family = new Family();
        family.setId(100L);

        User user = new User();
        user.setId(200L);
        user.setName("Maria");

        Expense expense = new Expense();
        expense.setId(30L);
        expense.setAmount(new BigDecimal("75.00"));
        expense.setDescription("Dinner");
        expense.setExpenseDate(LocalDateTime.of(2024, 8, 10, 19, 0));
        expense.setFamily(family);
        expense.setUser(user);
        expense.setRawInput("Dinner out");
        expense.setSourceType("manual");

        ExpenseEntity entity = mapper.toExpenseEntity(expense);

        assertEquals(100L, entity.getFamilyId());
        assertEquals(200L, entity.getUserId());
    }
}
