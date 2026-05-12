package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.entity.ExpenseOCREntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseOCRRepository extends JpaRepository<ExpenseOCREntity, Long> {
}