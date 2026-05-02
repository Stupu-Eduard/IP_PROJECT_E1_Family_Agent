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

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "last_active")
    private LocalDateTime lastActive;

    // Adăugăm un câmp virtual sau calculat pentru expirare dacă este nevoie, 
    // dar momentan ne bazăm pe structura lor oficială.
    public boolean isValid() {
        return lastActive != null && lastActive.isAfter(LocalDateTime.now().minusDays(1));
    }
}
