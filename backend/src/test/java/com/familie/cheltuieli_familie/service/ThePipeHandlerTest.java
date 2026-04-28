package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ThePipeHandlerTest {

    private ThePipeHandler thePipeHandler;

    @Mock
    private WebSocketSession session1;

    @Mock
    private WebSocketSession session2;

    @BeforeEach
    void setUp() {
        thePipeHandler = new ThePipeHandler();
    }

    @Test
    void broadcast_trimiteMesajCatreToateSesiunileDeschise() throws IOException {
        // GIVEN
        String payload = "{\"lat\": 47.1, \"lng\": 27.2}";
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionEstablished(session2);

        // WHEN
        thePipeHandler.broadcast(payload);

        // THEN
        verify(session1, times(1)).sendMessage(new TextMessage(payload));
        verify(session2, times(1)).sendMessage(new TextMessage(payload));
    }

    @Test
    void broadcast_nuTrimiteMesajCatreSesiunileInchiase() throws IOException {
        // GIVEN
        String payload = "{\"status\": \"offline\"}";
        when(session1.isOpen()).thenReturn(false);

        thePipeHandler.afterConnectionEstablished(session1);

        // WHEN
        thePipeHandler.broadcast(payload);

        // THEN
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_eliminaSesiuneaDupaInchidere() throws IOException {
        // ... (existent)
    }

    @Test
    void handleTextMessage_LogheazaMesajulPrimit() {
        // GIVEN
        TextMessage message = new TextMessage("Salut de la client");
        
        // WHEN & THEN (verificam doar ca nu arunca exceptie, metoda face doar logging)
        assertDoesNotThrow(() -> thePipeHandler.handleTextMessage(session1, message));
    }
}
