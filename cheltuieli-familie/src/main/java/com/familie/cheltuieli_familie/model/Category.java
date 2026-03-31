package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categorii")
@Data
public class Category {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nume;

    @ManyToOne
    @JoinColumn(name = "parent_id")
    private Category parent;
}