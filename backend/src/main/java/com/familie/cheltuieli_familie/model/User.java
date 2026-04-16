package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String nume;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_h", nullable = false) // schimbare nume coloana
    private String parola;
}