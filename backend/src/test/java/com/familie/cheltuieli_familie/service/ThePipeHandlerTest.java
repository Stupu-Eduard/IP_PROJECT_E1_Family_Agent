package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.Map;

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
        when(session1.getAttributes()).thenReturn(Map.of("familyId", 10L));
    }

    @Test
    void sendToParent_trimiteMesajDoarCatreParinteleTargetat() throws IOException {
        // GIVEN
        when(session2.getAttributes()).thenReturn(Map.of("familyId", 20L));
        String payload = "{\"lat\": 47.1, \"lng\": 27.2, \"childId\": 5}";
        when(session1.isOpen()).thenReturn(true);

        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionEstablished(session2);

        // WHEN
        thePipeHandler.sendToParent(10L, payload);

        // THEN
        verify(session1, times(1)).sendMessage(new TextMessage(payload));
        verify(session2, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToParent_nuTrimiteNimicDacaFamiliaNuEConectata() throws IOException {
        // GIVEN
        thePipeHandler.afterConnectionEstablished(session1);

        // WHEN
        thePipeHandler.sendToParent(99L, "payload");

        // THEN
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToParent_nuTrimiteCareSesiuniInchise() throws IOException {
        // GIVEN
        when(session1.isOpen()).thenReturn(false);
        thePipeHandler.afterConnectionEstablished(session1);

        // WHEN
        thePipeHandler.sendToParent(10L, "payload");

        // THEN
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToParent_eliminaSesiuneaDupaInchidere() throws IOException {
        // GIVEN
        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionClosed(session1, org.springframework.web.socket.CloseStatus.NORMAL);

        // WHEN
        thePipeHandler.sendToParent(10L, "payload");

        // THEN
        verify(session1, never()).isOpen();
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToParent_continuaCandOSesiuneEsueaza() throws Exception {
        // GIVEN - doua sesiuni ale aceleiasi familii
        when(session2.getAttributes()).thenReturn(Map.of("familyId", 10L));
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionEstablished(session2);

        doThrow(new IOException("Fail")).when(session1).sendMessage(any());

        // WHEN
        assertDoesNotThrow(() -> thePipeHandler.sendToParent(10L, "payload"));

        // THEN - a doua sesiune a aceleiasi familii trebuie sa primeasca mesajul
        verify(session2, times(1)).sendMessage(any());
    }

    @Test
    void broadcast_trimiteLaToateFamiliile() throws IOException {
        // GIVEN
        when(session2.getAttributes()).thenReturn(Map.of("familyId", 20L));
        String payload = "{\"status\": \"general\"}";
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
    void afterConnectionEstablished_ignoraSesiuneaFaraFamilyId() throws IOException {
        // GIVEN
        when(session1.getAttributes()).thenReturn(Map.of());

        thePipeHandler.afterConnectionEstablished(session1);

        // WHEN
        thePipeHandler.broadcast("payload");

        // THEN - sesiunea fara familyId nu e adaugata
        verify(session1, never()).isOpen();
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }
}
