package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.entity.TransactionEntity;
import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;
import com.familie.cheltuieli_familie.repository.TransactionRepository;
import com.familie.cheltuieli_familie.validation.TransactionValidator;
import com.familie.cheltuieli_familie.validation.ValidationException;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class StorageManager implements StorageService {

    private final TransactionRepository transactionRepository;
    private final TransactionValidator validator;

    public StorageManager(TransactionRepository transactionRepository, TransactionValidator validator) {
        this.transactionRepository = transactionRepository;
        this.validator = validator;
    }

    @Override
    public StorageResult save(List<Transaction> transactions) {
        if (transactions == null || transactions.isEmpty()) {
            System.out.println("No transactions received for storage.");
            return new StorageResult(0, 0, 0);
        }

        int saved = 0;
        int failed = 0;

        for (Transaction transaction : transactions) {
            try {
                validator.validate(transaction);

                TransactionEntity entity = new TransactionEntity(
                        transaction.getDate(),
                        transaction.getAmount(),
                        transaction.getDescription(),
                        transaction.getType(),
                        transaction.getCurrency()
                );

                transactionRepository.save(entity);
                saved++;

                System.out.println("Saved transaction: " + transaction);

            } catch (ValidationException e) {
                failed++;
                System.out.println("Validation error: " + e.getMessage());
            } catch (Exception e) {
                failed++;
                System.out.println("Unexpected storage error: " + e.getMessage());
            }
        }

        return new StorageResult(transactions.size(), saved, failed);
    }
}