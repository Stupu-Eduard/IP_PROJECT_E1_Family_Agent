package com.familie.cheltuieli_familie.service;

import com.fasterxml.jackson.databind.JsonNode;
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
    public void listenToEvents() {
        try {
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    log.info("ℹ️ PostgresNotificationListener dezactivat (Mediu H2/Test detectat).");
                    return;
                }
            }

            log.info("📡 Pornesc 'Inima Sistemului': Postgres LISTEN activat.");
            try (Connection connection = dataSource.getConnection()) {
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                try (Statement stmt = connection.createStatement()) {
                    // Ascultăm pe canale specifice
                    stmt.execute("LISTEN location_updates");
                    stmt.execute("LISTEN general_notifications");
                }

                while (!Thread.currentThread().isInterrupted()) {
                    PGNotification[] notifications = pgConnection.getNotifications(500);
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            processNotification(notification.getName(), notification.getParameter());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Eroare critică în PostgresNotificationListener: ", e);
        }
    }

    private void processNotification(String channel, String payload) {
        log.debug("🔔 Eveniment DB pe canalul [{}]: {}", channel, payload);
        try {
            // Putem adăuga logică de filtrare aici
            JsonNode node = objectMapper.readTree(payload);
            
            // Exemplu de filtrare/augmentare pentru locații
            if ("location_updates".equals(channel)) {
                // Dacă payload-ul este doar un ID, putem face un fetch, 
                // dar conform V33, row_to_json trimite tot rândul.
                thePipeHandler.broadcast(payload);
            } else {
                // Trimitere generică pentru alte canale
                thePipeHandler.broadcast(payload);
            }
            
        } catch (Exception e) {
            log.error("❌ Eroare procesare payload DB: {}", e.getMessage());
        }
    }
}
