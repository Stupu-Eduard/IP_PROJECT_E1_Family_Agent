package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {
}