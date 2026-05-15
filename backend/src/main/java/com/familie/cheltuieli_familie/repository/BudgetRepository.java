package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Budget;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Optional;

@Repository
public interface BudgetRepository extends JpaRepository<Budget, Long> {

    @Query("SELECT COALESCE(SUM(b.amount), 0) FROM Budget b WHERE b.family.id = :familyId AND b.startDate <= :today AND b.endDate >= :today")
    BigDecimal sumActiveBudgetsForFamily(@Param("familyId") Long familyId, @Param("today") LocalDate today);

    @Query("SELECT b FROM Budget b WHERE b.user.id = :userId AND b.category IS NULL AND b.startDate <= :today AND b.endDate >= :today")
    Optional<Budget> findChildBudget(@Param("userId") Long userId, @Param("today") LocalDate today);
}
