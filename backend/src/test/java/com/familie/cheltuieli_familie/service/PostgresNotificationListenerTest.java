package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.PGConnection;

import javax.sql.DataSource;
import java.lang.reflect.Method;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.Statement;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class PostgresNotificationListenerTest {

    @Mock
    private DataSource dataSource;
    @Mock
    private Connection connection;
    @Mock
    private DatabaseMetaData metaData;
    @Mock
    private PGConnection pgConnection;
    @Mock
    private Statement statement;
    @Mock
    private ThePipeHandler thePipeHandler;
    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private PostgresNotificationListener listener;

    @Test
    void listenToEvents_ShouldAbort_WhenNotPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDriverName()).thenReturn("H2 Driver");

        listener.listenToLocationUpdates();

        verify(connection, never()).unwrap(PGConnection.class);
    }

    @Test
    void listenToEvents_ShouldStartListening_WhenPostgres() throws Exception {
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(metaData);
        when(metaData.getDriverName()).thenReturn("PostgreSQL JDBC Driver");
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(connection.createStatement()).thenReturn(statement);

        // Simulate interruption to exit the loop immediately
        Thread.currentThread().interrupt();

        listener.listenToLocationUpdates();

        verify(statement).execute("LISTEN location_updates");
        verify(statement).execute("LISTEN general_notifications");
    }

    @Test
    void listenToEvents_ShouldLogAndRecover_WhenExceptionOccurs() throws Exception {
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Down"));

        listener.listenToLocationUpdates();

        // Should not crash, just log error
        verify(dataSource).getConnection();
    }

    @Test
    void processNotification_ShouldBroadcastPayload() throws Exception {
        String payload = "{\"test\": \"data\"}";
        
        // Use reflection to call private method for unit testing coverage
        Method method = PostgresNotificationListener.class.getDeclaredMethod("processNotification", String.class, String.class);
        method.setAccessible(true);
        method.invoke(listener, "location_updates", payload);

        verify(thePipeHandler).broadcast(payload);
    }

    @Test
    void processNotification_ShouldLogError_WhenInvalidJson() throws Exception {
        String payload = "invalid-json";
        when(objectMapper.readTree(payload)).thenThrow(new RuntimeException("JSON Error"));

        Method method = PostgresNotificationListener.class.getDeclaredMethod("processNotification", String.class, String.class);
        method.setAccessible(true);
        method.invoke(listener, "some-channel", payload);

        verify(thePipeHandler, never()).broadcast(anyString());
    }
}
