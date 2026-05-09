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
class ParserStorageIntegrationTest {

    @Autowired
    private StorageManager storageManager;

    @Autowired
    private ExpenseOCRRepository repository;

    private final BankStatementParser parser = new BankStatementParser();

    @Test
    void parserOutputShouldBeInsertedIntoMainDb() {
        String rawText = """
                10/03/2025 Kaufland Parser Integration Test 45.99
                11/03/2025 Lidl Parser Integration Test 100.50
                """;

        long before = repository.count();

        List<Transaction> transactions = parser.parseText(rawText);

        assertNotNull(transactions);
        assertFalse(transactions.isEmpty());
        assertEquals(2, transactions.size());

        StorageResult result = storageManager.save(transactions);

        long after = repository.count();

        assertEquals(transactions.size(), result.getTotalTransactions());
        assertEquals(transactions.size(), result.getSavedTransactions());
        assertEquals(0, result.getFailedTransactions());
        assertEquals(before + transactions.size(), after);
    }
}