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
        try {
            // Mai întâi verificăm ce bază de date avem activă
            try (Connection connection = dataSource.getConnection()) {
                if (!connection.getMetaData().getDriverName().contains("PostgreSQL")) {
                    log.info("ℹ️ PostgresNotificationListener dezactivat (Mediu H2/Test detectat).");
                    return;
                }
            }

            // Dacă am ajuns aici, avem Postgres, deci putem continua
            log.info("📡 Pornesc ascultarea evenimentelor Postgres LISTEN...");
            try (Connection connection = dataSource.getConnection()) {
                PGConnection pgConnection = connection.unwrap(PGConnection.class);
                try (Statement stmt = connection.createStatement()) {
                    stmt.execute("LISTEN location_updates");
                }

                while (!Thread.currentThread().isInterrupted()) {
                    PGNotification[] notifications = pgConnection.getNotifications(500);
                    if (notifications != null) {
                        for (PGNotification notification : notifications) {
                            processNotification(notification.getParameter());
                        }
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Eroare în PostgresNotificationListener: ", e);
        }
    }

    private void processNotification(String payload) {
        log.info("🔔 NOTIFICARE POSTGRES DETECTATĂ: {}", payload);
        try {
            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
            Long id = node.get("id").asLong();

            try (Connection conn = dataSource.getConnection();
                 java.sql.PreparedStatement pstmt = conn.prepareStatement(
                         "SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM locations WHERE id = ?")) {
                
                pstmt.setLong(1, id);
                try (java.sql.ResultSet rs = pstmt.executeQuery()) {
                    if (rs.next()) {
                        String finalJson = String.format("{\"id\": %d, \"lat\": %f, \"lng\": %f, \"type\": \"LIVE_UPDATE\"}",
                                id, rs.getDouble("lat"), rs.getDouble("lng"));
                        thePipeHandler.broadcast(finalJson);
                    }
                }
            }
        } catch (Exception e) {
            log.error("❌ Eroare transformare locație: {}", e.getMessage());
            thePipeHandler.broadcast(payload);
        }
    }

}
