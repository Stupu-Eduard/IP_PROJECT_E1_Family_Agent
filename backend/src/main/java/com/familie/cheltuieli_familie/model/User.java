package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

import java.time.LocalDate;

@Entity
@Table(name = "users")
@Data
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Column(unique = true, nullable = false)
    private String email;

    @Column(name = "password_h", nullable = false)
    private String passwordH;

    @Column(name = "created_at")
    private LocalDate createdAt;
}