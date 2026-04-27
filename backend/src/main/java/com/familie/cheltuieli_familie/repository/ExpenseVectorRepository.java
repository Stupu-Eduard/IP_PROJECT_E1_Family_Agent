package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.ExpenseEntity;

public interface ExpenseVectorRepository {
    void saveVector(ExpenseEntity entity, float[] vector);
    boolean existsInVectorStore(Long id);
}
