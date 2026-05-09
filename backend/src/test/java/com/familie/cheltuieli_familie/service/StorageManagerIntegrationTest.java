package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.ExpenseOCRRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.springframework.test.annotation.Rollback;
import org.springframework.test.context.TestPropertySource;

import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
@Import(StorageManager.class)
@Rollback(false)
@TestPropertySource(properties = {
        "spring.datasource.url=jdbc:postgresql://localhost:5432/main_db",
        "spring.datasource.username=FamilyDB",
        "spring.datasource.password=postgres",
        "spring.datasource.driver-class-name=org.postgresql.Driver",
        "spring.jpa.hibernate.ddl-auto=none",
        "spring.test.database.replace=none"
})
class StorageManagerIntegrationTest {

    @Autowired
    private StorageManager storageManager;

    @Autowired
    private ExpenseOCRRepository repository;

    @Test
    void saveShouldInsertTransactionIntoDatabase() {
        long before = repository.count();

        Transaction transaction = new Transaction(
                LocalDate.of(2025, 3, 10),
                "Lidl Integration Test",
                100.5,
                "RON",
                "EXPENSE"
        );

        StorageResult result = storageManager.save(List.of(transaction));

        long after = repository.count();

        assertEquals(1, result.getTotalTransactions());
        assertEquals(1, result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());
        assertEquals(before + 1, after);
    }
}