package com.familie.cheltuieli_familie.controller;

import com.familie.cheltuieli_familie.dto.LocationDto;
import com.familie.cheltuieli_familie.dto.UpdateLocationCoordinatesRequest;
import com.familie.cheltuieli_familie.model.Location;
import com.familie.cheltuieli_familie.repository.LocationRepository;
import com.familie.cheltuieli_familie.service.ThePipeHandler;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class LocationControllerTest {

    @Test
    void updateCoordinates_whenLocationExists_updatesAndReturnsDto() {
        LocationRepository locationRepository = mock(LocationRepository.class);
        ThePipeHandler thePipeHandler = mock(ThePipeHandler.class);
        LocationController controller = new LocationController(locationRepository, thePipeHandler);

        Location location = new Location();
        location.setId(5L);
        location.setStore("Mega");
        location.setCity("Iasi");
        location.setCountry("RO");

        when(locationRepository.updateCoordinates(5L, 46.1, 27.6)).thenReturn(1);
        when(locationRepository.findById(5L)).thenReturn(Optional.of(location));

        LocationDto dto = controller.updateCoordinates(5L, new UpdateLocationCoordinatesRequest(46.1, 27.6));

        verify(locationRepository).updateCoordinates(5L, 46.1, 27.6);
        verify(locationRepository).findById(5L);
        verify(thePipeHandler, times(1)).broadcast(anyString());

        assertEquals(5L, dto.id());
        assertEquals("Mega", dto.store());
        assertNull(dto.address());
        assertEquals("Iasi", dto.city());
        assertEquals("RO", dto.country());
        assertEquals(46.1, dto.lat());
        assertEquals(27.6, dto.lng());
    }

    @Test
    void updateCoordinates_whenUpdateCountZero_throwsIllegalArgumentException() {
        LocationRepository locationRepository = mock(LocationRepository.class);
        ThePipeHandler thePipeHandler = mock(ThePipeHandler.class);
        LocationController controller = new LocationController(locationRepository, thePipeHandler);

        when(locationRepository.updateCoordinates(404L, 1.0, 2.0)).thenReturn(0);
		UpdateLocationCoordinatesRequest request = new UpdateLocationCoordinatesRequest(1.0, 2.0);

        assertThrows(IllegalArgumentException.class,
				() -> controller.updateCoordinates(404L, request));

        verify(locationRepository, never()).findById(anyLong());
        verify(thePipeHandler, never()).broadcast(anyString());
    }

    @Test
    void updateCoordinates_whenNotFoundAfterUpdate_throwsIllegalArgumentException() {
        LocationRepository locationRepository = mock(LocationRepository.class);
        ThePipeHandler thePipeHandler = mock(ThePipeHandler.class);
        LocationController controller = new LocationController(locationRepository, thePipeHandler);

        when(locationRepository.updateCoordinates(6L, 1.0, 2.0)).thenReturn(1);
        when(locationRepository.findById(6L)).thenReturn(Optional.empty());
		UpdateLocationCoordinatesRequest request = new UpdateLocationCoordinatesRequest(1.0, 2.0);

        assertThrows(IllegalArgumentException.class,
				() -> controller.updateCoordinates(6L, request));
    }
}
