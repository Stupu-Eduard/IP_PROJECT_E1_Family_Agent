package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

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
        lenient().when(session1.getAttributes()).thenReturn(Map.of("familyId", 10L));
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

    @Test
    void afterConnectionClosed_cuFamilyIdNull_nuAruncaExceptie() {
        // GIVEN - sesiune fara familyId in atribute
        when(session1.getAttributes()).thenReturn(Map.of());

        // WHEN / THEN - nu ar trebui sa arunce exceptie
        assertDoesNotThrow(() ->
                thePipeHandler.afterConnectionClosed(session1, CloseStatus.NORMAL));
    }

    @Test
    void afterConnectionClosed_cuFamilyIdCareNuExistaInMap_nuAruncaExceptie() {
        // GIVEN - sesiune cu familyId, dar nu a fost niciodata inregistrata
        when(session1.getAttributes()).thenReturn(Map.of("familyId", 99L));

        // WHEN / THEN - lista e null in map, nu ar trebui sa arunce exceptie
        assertDoesNotThrow(() ->
                thePipeHandler.afterConnectionClosed(session1, CloseStatus.NORMAL));
    }

    @Test
    void afterConnectionClosed_cuMaiMulteSesiuni_nuStergeIntrareaDinMap() throws IOException {
        // GIVEN - doua sesiuni pentru aceeasi familie
        when(session2.getAttributes()).thenReturn(Map.of("familyId", 10L));
        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionEstablished(session2);

        // WHEN - inchidem doar prima sesiune
        thePipeHandler.afterConnectionClosed(session1, CloseStatus.NORMAL);

        // THEN - session2 inca primeste mesaje (familia nu e stearsa din map)
        when(session2.isOpen()).thenReturn(true);
        thePipeHandler.sendToParent(10L, "payload");
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }

    @Test
    void sendToParent_nuTrimiteNimicCandListaEGoala() throws IOException {
        // GIVEN - lista goala (nu null) in map pentru familia 10
        ConcurrentHashMap<Long, List<WebSocketSession>> map = new ConcurrentHashMap<>();
        map.put(10L, new CopyOnWriteArrayList<>());
        ReflectionTestUtils.setField(thePipeHandler, "sessionsByFamily", map);

        // WHEN
        thePipeHandler.sendToParent(10L, "payload");

        // THEN
        verify(session1, never()).sendMessage(any(TextMessage.class));
    }

    @Test
    void broadcast_continuesCandOSesiuneAruncaIOException() throws IOException {
        // GIVEN
        when(session2.getAttributes()).thenReturn(Map.of("familyId", 20L));
        when(session1.isOpen()).thenReturn(true);
        when(session2.isOpen()).thenReturn(true);

        thePipeHandler.afterConnectionEstablished(session1);
        thePipeHandler.afterConnectionEstablished(session2);

        doThrow(new IOException("Fail")).when(session1).sendMessage(any());

        // WHEN / THEN - nu arunca exceptie, continua cu session2
        assertDoesNotThrow(() -> thePipeHandler.broadcast("payload"));
        verify(session2, times(1)).sendMessage(any(TextMessage.class));
    }
}
