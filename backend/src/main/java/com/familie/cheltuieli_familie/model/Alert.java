package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "alerts")
@Data
public class Alert {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "child_id", nullable = false)
    private Long childId;

    @Column(name = "parent_id", nullable = false)
    private Long parentId;

    @Column(nullable = false)
    private String message;

    @Column(name = "restricted_category", nullable = false)
    private String restrictedCategory;

    @Column(nullable = false)
    private LocalDateTime timestamp;

    @Column(nullable = false)
    private boolean read = false;

    @Column(name = "extra_cost")
    private java.math.BigDecimal extraCost = java.math.BigDecimal.ZERO;
}