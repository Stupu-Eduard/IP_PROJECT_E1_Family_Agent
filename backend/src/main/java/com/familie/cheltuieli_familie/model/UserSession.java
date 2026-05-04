package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.Data;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
public class UserSession {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "session_token")
    private String sessionToken;

    @Column(name = "last_active")
    private LocalDateTime lastActive = LocalDateTime.now();
}