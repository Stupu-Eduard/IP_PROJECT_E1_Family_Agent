package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.model.StorageResult;
import com.familie.cheltuieli_familie.model.Transaction;

import java.util.List;

public interface StorageService {
    StorageResult save(List<Transaction> transactions);
}
