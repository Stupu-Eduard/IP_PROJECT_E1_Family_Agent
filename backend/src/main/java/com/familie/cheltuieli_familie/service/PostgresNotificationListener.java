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
                        
                        try {
                            // Parsăm JSON-ul primit de la Postgres
                            com.fasterxml.jackson.databind.JsonNode node = objectMapper.readTree(payload);
                            Long id = node.get("id").asLong();

                            // Deoarece coloana "location" este binară (WKB), cel mai sigur este să 
                            // cerem rapid bazei de date coordonatele clare pentru acest ID.
                            try (Connection conn = dataSource.getConnection();
                                 Statement stmt = conn.createStatement();
                                 java.sql.ResultSet rs = stmt.executeQuery(
                                         "SELECT ST_Y(location::geometry) as lat, ST_X(location::geometry) as lng FROM locations WHERE id = " + id)) {
                                
                                if (rs.next()) {
                                    double lat = rs.getDouble("lat");
                                    double lng = rs.getDouble("lng");
                                    
                                    // Construim JSON-ul final pe care îl înțelege Google Maps
                                    String finalJson = String.format(
                                        "{\"id\": %d, \"lat\": %f, \"lng\": %f, \"type\": \"LIVE_UPDATE\"}",
                                        id, lat, lng
                                    );
                                    
                                    log.info("🚀 Redirecționez coordonate clare către hartă: {}", finalJson);
                                    thePipeHandler.broadcast(finalJson);
                                }
                            }
                        } catch (Exception e) {
                            log.error("❌ Eroare la transformarea locației binare: {}", e.getMessage());
                            // Fallback: trimitem payload-ul original în caz de eroare (deși harta s-ar putea să-l ignore)
                            thePipeHandler.broadcast(payload);
                        }
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
