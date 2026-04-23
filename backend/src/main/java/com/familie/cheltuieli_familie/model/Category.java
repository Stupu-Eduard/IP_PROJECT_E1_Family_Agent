package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categories")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(name = "description", length = 150)
    private String description;

    @Column(name = "is_active")
    private Boolean isActive = false;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;
}