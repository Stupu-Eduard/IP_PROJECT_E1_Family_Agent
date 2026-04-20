package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.security.service.LocationStreamService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationStreamServiceTest {

    private LocationStreamService locationStreamService;

    @BeforeEach
    void setUp() {
        // Instanțiem serviciul real
        locationStreamService = new LocationStreamService();
    }

    @Test
    void testSubscribeParent_CreatesAndStoresEmitter() {
        Long parentId = 1L;

        // Act
        SseEmitter emitter = locationStreamService.subscribeParent(parentId);

        // Assert
        assertNotNull(emitter, "Emitter-ul nu ar trebui să fie null");

        // Verificăm prin Reflection dacă a fost adăugat în map-ul privat
        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> map = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(locationStreamService, "parentEmitters");

        assertNotNull(map);
        assertTrue(map.containsKey(parentId), "Parintele ar trebui să fie în map");
        assertEquals(emitter, map.get(parentId));
    }

    @Test
    void testSendLocationToParent_WhenNoParentConnected() {
        // Act & Assert
        // Dacă parentId nu există în map, nu trebuie să arunce eroare (intră pe ramura if emitter == null)
        assertDoesNotThrow(() -> locationStreamService.sendLocationToParent(999L, 45.0, 25.0));
    }

    @Test
    void testSendLocationToParent_Success() throws IOException {
        Long parentId = 5L;
        // Cream un mock pentru emitter ca să verificăm dacă primește datele
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // Injectăm manual mock-ul în map-ul privat al serviciului
        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        // Act
        locationStreamService.sendLocationToParent(parentId, 44.42, 26.10);

        // Assert
        // Verificăm dacă s-a apelat metoda send() cu orice tip de eveniment
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testSendLocationToParent_HandlesIOException() throws IOException {
        Long parentId = 10L;
        SseEmitter mockEmitter = mock(SseEmitter.class);

        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        // Simulăm o eroare de rețea (IOException)
        doThrow(new IOException("Connection reset")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // Act
        locationStreamService.sendLocationToParent(parentId, 44.42, 26.10);

        // Assert
        // Verificăm dacă, în caz de eroare, serviciul a scos părintele din listă (catch block)
        assertFalse(mockMap.containsKey(parentId), "Parintele ar trebui scos din map dacă trimiterea eșuează");
    }
}