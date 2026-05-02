package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
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

    @InjectMocks
    private PostgresNotificationListener listener;

    @Mock
    private java.sql.DatabaseMetaData databaseMetaData;

    @Test
    void listenToLocationUpdates_ArTrebuiaSaRedirectionezeNotificarileCatreThePipe() throws Exception {
        // GIVEN
        when(dataSource.getConnection()).thenReturn(connection);
        when(connection.getMetaData()).thenReturn(databaseMetaData);
        when(databaseMetaData.getDriverName()).thenReturn("PostgreSQL Driver");
        when(connection.unwrap(PGConnection.class)).thenReturn(pgConnection);
        when(connection.createStatement()).thenReturn(statement);

        // Simulăm o notificare
        PGNotification mockNotification = mock(PGNotification.class);
        when(mockNotification.getParameter()).thenReturn("{\"lat\": 1.0, \"lng\": 2.0}");
        
        // Prima dată returnăm o notificare, a doua oară întrerupem thread-ul pentru a ieși din while
        when(pgConnection.getNotifications(anyInt()))
                .thenReturn(new PGNotification[]{mockNotification})
                .thenAnswer(invocation -> {
                    Thread.currentThread().interrupt();
                    return null;
                });

        // WHEN
        listener.listenToLocationUpdates();

        // THEN
        verify(statement).execute("LISTEN location_updates");
        verify(thePipeHandler).broadcast("{\"lat\": 1.0, \"lng\": 2.0}");
    }

    @Test
    void listenToLocationUpdates_ArTrebuiaSaGestionezeEroareaDeConexiune() throws Exception {
        // GIVEN
        when(dataSource.getConnection()).thenThrow(new RuntimeException("DB Down"));
        
        // Întrerupem imediat thread-ul pentru a evita loop-ul de sleep/retry infinit în test
        Thread.currentThread().interrupt();

        // WHEN
        listener.listenToLocationUpdates();

        // THEN - verificăm că nu a crăpat și a logat eroarea (implicit prin faptul că testul trece)
        verifyNoInteractions(thePipeHandler);
    }
}
