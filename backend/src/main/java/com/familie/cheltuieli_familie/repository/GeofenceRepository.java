package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.GeofenceZone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface GeofenceRepository extends JpaRepository<GeofenceZone, Long> {
    Optional<GeofenceZone> findByParentIdAndIsActiveTrue(Long parentId);
}