package com.proiect.repository;

import com.proiect.model.ExpenseEntity;

public interface ExpenseVectorRepository {
    void saveVector(ExpenseEntity entity, float[] vector);
    boolean existsInVectorStore(Long id);
}
