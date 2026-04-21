package org.example.storage;

import org.example.model.Transaction;

import java.util.List;

public interface StorageService {
    StorageResult save(List<Transaction> transactions);
}