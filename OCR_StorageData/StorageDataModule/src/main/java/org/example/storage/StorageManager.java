package org.example.storage;

import org.example.db.DatabaseManager;
import org.example.mapper.TransactionJsonMapper;
import org.example.model.Transaction;
import org.example.validation.TransactionValidator;
import org.example.validation.ValidationException;
import java.util.List;

public class StorageManager implements StorageService {

    private final DatabaseManager databaseManager;
    private final TransactionValidator validator;
    private final TransactionJsonMapper mapper;

    public StorageManager(DatabaseManager databaseManager, TransactionValidator validator, TransactionJsonMapper mapper) {
        this.databaseManager = databaseManager;
        this.validator = validator;
        this.mapper = mapper;
    }

    @Override
    public StorageResult save(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("Nici o tranzactie primita.");
            return new StorageResult(0, 0, 0);
        }

        int saved = 0;
        int failed = 0;

        databaseManager.connect();

        for (Transaction transaction : transactions) {
            try {
                validator.validate(transaction);
                String payload = mapper.toJson(transaction);
                databaseManager.send(payload);
                saved++;

            } catch (ValidationException e) {
                failed++;
                System.out.println("Validation error: " + e.getMessage());
            } catch (Exception e) {
                failed++;
                System.out.println("Unexpected storage error: " + e.getMessage());
            }
        }

        databaseManager.disconnect();

        return new StorageResult(transactions.size(), saved, failed);
    }
}