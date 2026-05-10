package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.RevokedToken;
import com.familie.cheltuieli_familie.repository.RevokedTokenRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;

@Service
@RequiredArgsConstructor
public class TokenBlacklistService {

    private final RevokedTokenRepository revokedTokenRepository;

    public void revokeToken(String jti, Date expirationDate) {
        LocalDateTime expiresAt = expirationDate.toInstant()
                .atZone(ZoneId.systemDefault())
                .toLocalDateTime();
                
        RevokedToken token = RevokedToken.builder()
                .jti(jti)
                .revokedAt(LocalDateTime.now())
                .expiresAt(expiresAt)
                .build();
        revokedTokenRepository.save(token);
    }

    public boolean isBlacklisted(String jti) {
        if (jti == null) return false;
        return revokedTokenRepository.existsByJti(jti);
    }
}
