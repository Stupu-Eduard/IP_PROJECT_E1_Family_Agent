package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Family;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface FamilyRepository extends JpaRepository<Family, Long> {
}