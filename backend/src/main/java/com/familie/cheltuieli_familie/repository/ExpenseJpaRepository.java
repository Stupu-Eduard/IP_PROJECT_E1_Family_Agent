package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Expense;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface ExpenseJpaRepository extends JpaRepository<Expense, Long> {

    Page<Expense> findByAiCategory(String aiCategory, Pageable pageable);

    Page<Expense> findByAiPerson(String aiPerson, Pageable pageable);

    @Query("SELECT e FROM Expense e WHERE e.expenseDate >= :from AND e.expenseDate < :to")
    Page<Expense> findByExpenseDateRange(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to, Pageable pageable);

    default Page<Expense> findByDateRange(LocalDate from, LocalDate to, Pageable pageable) {
        return findByExpenseDateRange(from.atStartOfDay(), to.plusDays(1).atStartOfDay(), pageable);
    }

    @Query("SELECT e FROM Expense e WHERE e.expenseDate >= :from AND e.expenseDate < :to")
    List<Expense> findByExpenseDateBetween(@Param("from") LocalDateTime from, @Param("to") LocalDateTime to);

    default List<Expense> findByDateBetween(LocalDate from, LocalDate to) {
        return findByExpenseDateBetween(from.atStartOfDay(), to.plusDays(1).atStartOfDay());
    }
}
