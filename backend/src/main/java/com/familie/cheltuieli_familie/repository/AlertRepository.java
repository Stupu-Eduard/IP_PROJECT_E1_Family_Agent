package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Alert;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AlertRepository extends JpaRepository<Alert, Long> {
    List<Alert> findByParentIdOrderByTimestampDesc(Long parentId);
    List<Alert> findByParentIdAndReadFalseOrderByTimestampDesc(Long parentId);

    // Metoda nouă pentru validarea CSRF
    boolean existsByRestrictedCategory(String restrictedCategory);

    List<Alert> findByTimestampBetween(java.time.LocalDateTime start, java.time.LocalDateTime end);
}