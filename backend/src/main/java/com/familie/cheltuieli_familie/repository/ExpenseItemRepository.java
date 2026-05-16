package com.familie.cheltuieli_familie.repository;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import com.familie.cheltuieli_familie.model.ExpenseItem;

import java.util.List;

@Repository
public interface ExpenseItemRepository extends JpaRepository<ExpenseItem, Long> {
    List<ExpenseItem> findByExpenseId(Long expenseId);
}
