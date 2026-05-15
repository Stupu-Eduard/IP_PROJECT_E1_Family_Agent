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
	@Query(value = """
          UPDATE locations
          SET location = ST_SetSRID(ST_MakePoint(:lng, :lat), 4326)\\:\\:geography
          WHERE id = :id
          """, nativeQuery = true)
	int updateCoordinates(@Param("id") Long id, @Param("lat") double lat, @Param("lng") double lng);

	@Query(value = """
    SELECT ST_Y(location\\:\\:geometry) as lat, ST_X(location\\:\\:geometry) as lng 
    FROM locations 
    WHERE id = :id
    """, nativeQuery = true)
	java.util.Map<String, Object> getRawCoordinates(@Param("id") Long id);
}
