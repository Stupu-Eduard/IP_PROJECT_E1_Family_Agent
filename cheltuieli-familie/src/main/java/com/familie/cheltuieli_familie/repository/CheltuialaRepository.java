package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Cheltuiala;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface CheltuialaRepository extends JpaRepository<Cheltuiala, Long> {
}