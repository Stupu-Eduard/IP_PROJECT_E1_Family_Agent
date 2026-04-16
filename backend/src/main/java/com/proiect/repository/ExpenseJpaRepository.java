package com.proiect.repository;

import com.proiect.model.ExpenseEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Standard JPA repository for ExpenseEntity.
 */
@Repository
public interface ExpenseJpaRepository extends JpaRepository<ExpenseEntity, Long> {
}
