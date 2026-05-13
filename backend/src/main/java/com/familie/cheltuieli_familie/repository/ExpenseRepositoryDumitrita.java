package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.ExpenseEntityDumitrita;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ExpenseRepositoryDumitrita extends JpaRepository<ExpenseEntityDumitrita, Long> {
}
