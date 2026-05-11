package com.proiect.repository;

import com.proiect.model.ExpenseEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface ExpenseJpaRepository extends JpaRepository<ExpenseEntity, Long> {

    Page<ExpenseEntity> findByCategory(String category, Pageable pageable);

    Page<ExpenseEntity> findByPerson(String person, Pageable pageable);

    @Query("SELECT e FROM ExpenseEntity e WHERE e.date BETWEEN :from AND :to")
    Page<ExpenseEntity> findByDateRange(LocalDate from, LocalDate to, Pageable pageable);

    List<ExpenseEntity> findByDateBetween(LocalDate from, LocalDate to);
}
