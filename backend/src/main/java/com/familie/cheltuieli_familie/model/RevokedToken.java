package com.familie.cheltuieli_familie.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

import java.time.Instant;

@Entity
@Table(name = "revoked_tokens")
public class RevokedToken
{

    @Id
    @Column(name = "jti", nullable = false, length = 255)
    private String jti;

    @Column(name = "revoked_at", nullable = false)
    private Instant revokedAt;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    protected RevokedToken() {
    }

    public RevokedToken(String jti, Instant revokedAt, Instant expiresAt) {
        this.jti = jti;
        this.revokedAt = revokedAt;
        this.expiresAt = expiresAt;
    }

    public String getJti() {
        return jti;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }
}