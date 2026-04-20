package com.familie.cheltuieli_familie.security.service;

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
        // Aici nu folosim @Mock sau @InjectMocks pentru ca serviciul nu are alte dependente (gen Repository)
        locationStreamService = new LocationStreamService();
    }

    @Test
    void testSubscribeParent() {
        Long parentId = 1L;

        // Act
        SseEmitter emitter = locationStreamService.subscribeParent(parentId);

        // Assert
        assertNotNull(emitter);

        // Folosim Reflection pentru a accesa map-ul privat si a verifica daca a intrat conexiunea
        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> parentEmitters = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(locationStreamService, "parentEmitters");

        assertNotNull(parentEmitters);
        assertTrue(parentEmitters.containsKey(parentId));
        assertEquals(emitter, parentEmitters.get(parentId));

        // Declansam finalizarea conexiunii manual pentru a acoperi si liniile cu lambdas (emitter.onCompletion)
        emitter.complete();
    }

    @Test
    void testSendLocationToParent_WhenParentNotConnected() {
        // Act & Assert: Daca parintele nu e in lista, ar trebui sa treaca prin "if (emitter == null)" si sa dea return
        // Verificam ca pur si simplu nu arunca absolut nicio eroare si trece linistit mai departe
        assertDoesNotThrow(() -> locationStreamService.sendLocationToParent(99L, 45.0, 25.0));
    }

    @Test
    void testSendLocationToParent_Success() throws IOException {
        Long parentId = 2L;
        // Cream un emitter "fals" ca sa ii putem urmari comportamentul
        SseEmitter mockEmitter = mock(SseEmitter.class);

        // Injectam map-ul falsificat direct in serviciu
        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        // Act
        locationStreamService.sendLocationToParent(parentId, 44.42, 26.10);

        // Assert: Verificam ca serviciul a apelat o singura data metoda send() pe emitterul fals
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testSendLocationToParent_ThrowsIOException() throws IOException {
        Long parentId = 3L;
        SseEmitter mockEmitter = mock(SseEmitter.class);

        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        // Simulam o picare a internetului la client (Arunca IOException cand incearca sa trimita date)
        doThrow(new IOException("Conexiune inchisa brusc")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        // Act
        locationStreamService.sendLocationToParent(parentId, 44.42, 26.10);

        // Assert: Blocul "catch" ar fi trebuit sa prinda exceptia si sa stearga parintele din lista
        assertFalse(mockMap.containsKey(parentId), "Parintele trebuia sters din map in momentul in care conexiunea a picat");
    }
}