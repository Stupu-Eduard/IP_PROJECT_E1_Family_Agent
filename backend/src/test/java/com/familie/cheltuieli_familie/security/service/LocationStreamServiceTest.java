package com.familie.cheltuieli_familie.security.service;

import com.familie.cheltuieli_familie.service.LocationAdapterService;
import com.familie.cheltuieli_familie.dto.LocationMapDto;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationStreamServiceTest {

    private LocationStreamService locationStreamService;

    // Mock pentru LocationAdapterService - acum e dependenta obligatorie
    private LocationAdapterService locationAdapterService;

    @BeforeEach
    void setUp() {
        // Cream mock-ul pentru adaptor
        locationAdapterService = mock(LocationAdapterService.class);

        // Instanțiem serviciul cu dependenta injectata
        locationStreamService = new LocationStreamService(locationAdapterService);

        // Configuram mock-ul sa returneze un DTO valid la orice apel
        when(locationAdapterService.adapt(anyLong(), anyLong(), anyDouble(), anyDouble(), anyList()))
                .thenReturn(new LocationMapDto(2L, 1L, 47.1585, 27.6014, false, LocalDateTime.now()));
    }

    @Test
    void testSubscribeParent_CreatesAndStoresEmitter() {
        Long parentId = 1L;

        SseEmitter emitter = locationStreamService.subscribeParent(parentId);

        assertNotNull(emitter, "Emitter-ul nu ar trebui sa fie null");

        @SuppressWarnings("unchecked")
        Map<Long, SseEmitter> map = (Map<Long, SseEmitter>) ReflectionTestUtils.getField(locationStreamService, "parentEmitters");

        assertNotNull(map);
        assertTrue(map.containsKey(parentId), "Parintele ar trebui sa fie in map");
        assertEquals(emitter, map.get(parentId));
    }

    @Test
    void testSendLocationToParent_WhenNoParentConnected() {
        // Daca parentId nu exista in map, nu trebuie sa arunce eroare
        assertDoesNotThrow(() ->
                locationStreamService.sendLocationToParent(2L, 999L, 45.0, 25.0, List.of())
        );
    }

    @Test
    void testSendLocationToParent_Success() throws IOException {
        Long parentId = 5L;
        SseEmitter mockEmitter = mock(SseEmitter.class);

        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        locationStreamService.sendLocationToParent(2L, parentId, 44.42, 26.10, List.of("restaurant"));

        // Verificam ca s-a apelat send() cu datele din adaptor
        verify(mockEmitter, times(1)).send(any(SseEmitter.SseEventBuilder.class));
    }

    @Test
    void testSendLocationToParent_HandlesIOException() throws IOException {
        Long parentId = 10L;
        SseEmitter mockEmitter = mock(SseEmitter.class);

        Map<Long, SseEmitter> mockMap = new ConcurrentHashMap<>();
        mockMap.put(parentId, mockEmitter);
        ReflectionTestUtils.setField(locationStreamService, "parentEmitters", mockMap);

        // Simulam o eroare de retea
        doThrow(new IOException("Connection reset")).when(mockEmitter).send(any(SseEmitter.SseEventBuilder.class));

        locationStreamService.sendLocationToParent(2L, parentId, 44.42, 26.10, List.of("restaurant"));

        // In caz de eroare, parintele trebuie scos din map
        assertFalse(mockMap.containsKey(parentId), "Parintele ar trebui scos din map daca trimiterea esueaza");
    }
}