package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "categorii")
@Data
public class Categorie {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nume;
}