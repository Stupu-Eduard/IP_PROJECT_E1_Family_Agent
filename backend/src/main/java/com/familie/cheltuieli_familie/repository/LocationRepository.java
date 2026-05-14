package com.familie.cheltuieli_familie.repository;

import com.familie.cheltuieli_familie.model.Location;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
public interface LocationRepository extends JpaRepository<Location, Long> {

    @Modifying
    @Transactional
    @Query("UPDATE Location l SET l.latitude = :lat, l.longitude = :lng WHERE l.id = :id")
    int updateCoordinates(@Param("id") Long id, @Param("lat") double lat, @Param("lng") double lng);
}
