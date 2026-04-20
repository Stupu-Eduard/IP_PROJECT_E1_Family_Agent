package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {
}