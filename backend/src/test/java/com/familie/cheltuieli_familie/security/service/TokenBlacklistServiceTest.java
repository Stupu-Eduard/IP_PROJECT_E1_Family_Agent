package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.model.RevokedToken;
import com.familie.cheltuieli_familie.repository.RevokedTokenRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TokenBlacklistServiceTest {

    @Mock
    private RevokedTokenRepository revokedTokenRepository;

    @InjectMocks
    private TokenBlacklistService tokenBlacklistService;

    @Test
    void shouldRevokeToken() {
        String jti = "test-jti";
        Date expiration = new Date(System.currentTimeMillis() + 3600000);

        tokenBlacklistService.revokeToken(jti, expiration);

        ArgumentCaptor<RevokedToken> captor = ArgumentCaptor.forClass(RevokedToken.class);
        verify(revokedTokenRepository).save(captor.capture());
        
        RevokedToken savedToken = captor.getValue();
        assertEquals(jti, savedToken.getJti());
        assertNotNull(savedToken.getRevokedAt());
        assertNotNull(savedToken.getExpiresAt());
    }

    @Test
    void shouldCheckIfTokenIsBlacklisted() {
        String jti = "blacklisted-jti";
        when(revokedTokenRepository.existsByJti(jti)).thenReturn(true);

        assertTrue(tokenBlacklistService.isBlacklisted(jti));
        
        verify(revokedTokenRepository).existsByJti(jti);
    }

    @Test
    void shouldReturnFalseIfNotBlacklisted() {
        String jti = "safe-jti";
        when(revokedTokenRepository.existsByJti(jti)).thenReturn(false);

        assertFalse(tokenBlacklistService.isBlacklisted(jti));
    }
}
