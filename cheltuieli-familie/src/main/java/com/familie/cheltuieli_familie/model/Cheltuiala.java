package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;
import java.math.BigDecimal;

@Entity
@Table(name = "cheltuieli")
@Data
public class Cheltuiala {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private BigDecimal suma;
    private String descriere;

    @ManyToOne
    @JoinColumn(name = "categorie_id")
    private Categorie categorie;
}