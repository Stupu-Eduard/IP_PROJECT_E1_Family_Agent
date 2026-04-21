package org.example.app;

import org.example.db.DatabaseManager;
import org.example.mapper.TransactionJsonMapper;
import org.example.model.Transaction;
import org.example.storage.StorageManager;
import org.example.storage.StorageResult;
import org.example.validation.TransactionValidator;

import java.util.ArrayList;
import java.util.List;

public class Main {

    public static void main(String[] args) {
        List<Transaction> transactions = new ArrayList<>();

        transactions.add(new Transaction("2025-03-10", -120.50, "Lidl", "expense", "RON"));
        transactions.add(new Transaction("2025-03-11", 5000.00, "Salary", "income", "RON"));
        transactions.add(new Transaction("2025-03-12", -49.99, "Netflix", "expense", "RON"));

        transactions.add(new Transaction("", 0, "", "expense", "RON"));

        DatabaseManager databaseManager = new DatabaseManager();
        TransactionValidator validator = new TransactionValidator();
        TransactionJsonMapper mapper = new TransactionJsonMapper();

        StorageManager storageManager = new StorageManager(databaseManager, validator, mapper);

        StorageResult result = storageManager.save(transactions);

        System.out.println("Rezultat final: " + result);
    }
}