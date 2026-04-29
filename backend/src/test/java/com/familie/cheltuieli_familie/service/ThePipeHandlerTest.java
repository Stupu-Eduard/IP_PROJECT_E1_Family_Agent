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
        // GIVEN
        String payload = "test";
        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionClosed(session1, org.springframework.web.socket.CloseStatus.NORMAL);

        // WHEN
        thePipeHandler.broadcast(payload);

        // THEN
        verify(session1, never()).isOpen();
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void handleTextMessage_LogheazaMesajulPrimit() {
        // GIVEN
        TextMessage mockMessage = mock(TextMessage.class);
        when(mockMessage.getPayload()).thenReturn("Salut de la client");
        
        // WHEN
        thePipeHandler.handleTextMessage(session1, mockMessage);

        // THEN
        // Verificam ca s-a interactionat cu mesajul pentru a extrage payload-ul (pentru logare)
        verify(mockMessage, atLeastOnce()).getPayload();
        // Verificam ca nu s-a facut nimic neasteptat cu sesiunea
        verifyNoInteractions(session1);
    }

    @Test
    void broadcast_gestioneazaEroareaDeComunicare() throws IOException {
        // GIVEN
        String payload = "error-test";
        when(session1.isOpen()).thenReturn(true);
        // Simulam o eroare de I/O la trimitere
        doThrow(new IOException("Network failure")).when(session1).sendMessage(any(TextMessage.class));

        thePipeHandler.afterConnectionEstablished(session1);

        // WHEN & THEN
        // Metoda nu trebuie sa arunce exceptia mai departe (este prinsa in catch)
        assertDoesNotThrow(() -> thePipeHandler.broadcast(payload));
        verify(session1, times(1)).sendMessage(any(TextMessage.class));
    }
}
