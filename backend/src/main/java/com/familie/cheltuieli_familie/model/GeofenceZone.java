package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.locationtech.jts.geom.Polygon;

@Entity
@Table(name = "geofence_zones")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class GeofenceZone {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "parent_id")
    private Long parentId;

    private String name;
    private String description;

    @Column(columnDefinition = "geometry(Polygon, 4326)")
    private Polygon area;

    @Builder.Default
    private boolean isActive = true;
}