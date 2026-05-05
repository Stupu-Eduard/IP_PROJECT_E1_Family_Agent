package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class PostgresNotificationListenerTest {

    @Mock
    private DataSource dataSource;

    @Mock
    private ThePipeHandler thePipeHandler;

    @Mock
    private Connection connection;

    @Mock
    private PGConnection pgConnection;

    @Mock
    private Statement statement;

    @Mock
    private PreparedStatement preparedStatement;

    @Mock
    private ResultSet resultSet;

    @InjectMocks
    private PostgresNotificationListener listener;

    @Mock
    private java.sql.DatabaseMetaData databaseMetaData;

    @Test
    void processNotification_ShouldHandleInvalidJson() throws Exception {
        // GIVEN
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL Driver");
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(connection.createStatement()).thenReturn(statement);

        PGNotification mockNotification = mock(PGNotification.class);
        when(mockNotification.getParameter()).thenReturn("{invalid}"); 
        
        when(pgConnection.getNotifications(anyInt()))
                .thenReturn(new PGNotification[]{mockNotification})
                .thenAnswer(inv -> { Thread.currentThread().interrupt(); return null; });
        
        // WHEN & THEN
        assertDoesNotThrow(() -> listener.listenToLocationUpdates());
    }

    @Test
    void processNotification_ShouldHandleMissingId() throws Exception {
        // GIVEN
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL Driver");
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(connection.createStatement()).thenReturn(statement);
        when(connection.prepareStatement(anyString())).thenReturn(preparedStatement);
        when(preparedStatement.executeQuery()).thenReturn(resultSet);

        PGNotification mockNotification = mock(PGNotification.class);
        when(mockNotification.getParameter()).thenReturn("{\"id\": 999}"); 
        
        when(pgConnection.getNotifications(anyInt()))
                .thenReturn(new PGNotification[]{mockNotification})
                .thenAnswer(inv -> { Thread.currentThread().interrupt(); return null; });

        when(resultSet.next()).thenReturn(false);

        // WHEN
        listener.listenToLocationUpdates();

        // THEN
        verify(thePipeHandler, never()).broadcast(anyString());
    }
}
