package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;

@Entity
@Table(name = "family_members")
@Data
public class FamilyMember {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id")
    private User user;

    @ManyToOne
    @JoinColumn(name = "family_id")
    private Family family;

    @Column(nullable = false)
    private String role;
}