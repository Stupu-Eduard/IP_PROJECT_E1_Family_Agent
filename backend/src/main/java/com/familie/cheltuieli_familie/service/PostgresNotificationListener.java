package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.postgresql.PGConnection;
import org.postgresql.PGNotification;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.Statement;

@Service
@Slf4j
@RequiredArgsConstructor
public class PostgresNotificationListener {

    private final DataSource dataSource;
    private final ThePipeHandler thePipeHandler;
    private final ObjectMapper objectMapper;

    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void listenToLocationUpdates() {
        log.info("📡 Pornesc ascultarea evenimentelor de tip Postgres LISTEN pe canalul 'location_updates'...");

        try (Connection connection = dataSource.getConnection()) {
            // Avem nevoie de conexiunea nativa Postgres pentru a folosi PGConnection
            PGConnection pgConnection = connection.unwrap(PGConnection.class);
            
            try (Statement stmt = connection.createStatement()) {
                stmt.execute("LISTEN location_updates");
            }

            while (!Thread.currentThread().isInterrupted()) {
                // Verificăm dacă există notificări noi (timeout 500ms pentru a nu bloca thread-ul la infinit)
                PGNotification[] notifications = pgConnection.getNotifications(500);

                if (notifications != null) {
                    for (PGNotification notification : notifications) {
                        String payload = notification.getParameter();
                        System.out.println("🔔 NOTIFICARE POSTGRES DETECTATĂ: " + payload);
                        log.debug("🔔 Notificare DB primită: {}", payload);
                        
                        thePipeHandler.broadcast(payload);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Eroare în PostgresNotificationListener: ", e);
            // Reîncercăm după o scurtă pauză în caz de eroare de conexiune
            try {
                Thread.sleep(5000);
                listenToLocationUpdates();
            } catch (InterruptedException ex) {
                Thread.currentThread().interrupt();
            }
        }
    }
}
