package com.familie.cheltuieli_familie.service;

import com.familie.cheltuieli_familie.exception.PipelineException;
import com.familie.cheltuieli_familie.repository.ExpenseJpaRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PipelineValidationServiceTest {

    @Mock
    private ExpenseJpaRepository repository;

    @Mock
    private QdrantVectorService qdrantVectorService;

    @InjectMocks
    private PipelineValidationService validationService;

    @Test
    void testValidatePersistenceSuccess() {
        when(repository.existsById(1L)).thenReturn(true);
        when(qdrantVectorService.existsInVectorStore(1L)).thenReturn(true);

        assertDoesNotThrow(() -> validationService.validatePersistence(1L));
    }

    @Test
    void testValidatePersistenceSqlFailure() {
        when(repository.existsById(1L)).thenReturn(false);

        PipelineException ex = assertThrows(PipelineException.class,
                () -> validationService.validatePersistence(1L));
        assertEquals("Entity not found in SQL Database after save!", ex.getMessage());
    }

    @Test
    void testValidatePersistenceVectorNotFound() {
        when(repository.existsById(1L)).thenReturn(true);
        when(qdrantVectorService.existsInVectorStore(1L)).thenReturn(false);

        assertDoesNotThrow(() -> validationService.validatePersistence(1L));
    }
}
