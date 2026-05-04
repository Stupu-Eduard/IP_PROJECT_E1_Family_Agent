package com.familie.cheltuieli_familie.model;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "user_sessions")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserSession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "session_token", length = 255)
    private String sessionToken;

    // NOU: Coloana creată de colegul tău în baza de date
    @Column(name = "csrf_token", length = 255)
    private String csrfToken;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    public boolean isValid() {
        return lastActive != null && lastActive.isAfter(LocalDateTime.now().minusDays(1));
    }
}