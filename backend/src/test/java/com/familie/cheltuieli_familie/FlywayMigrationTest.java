package com.familie.cheltuieli_familie;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@ActiveProfiles("test")
class FlywayMigrationTest {

    @Autowired
    private DataSource dataSource;

    @Test
    void verifyFlywayTables() throws SQLException {
        try (Connection conn = dataSource.getConnection()) {
            List<String> expectedTables = List.of(
                    "USERS",
                    "EXPENSES",
                    "CATEGORIES",
                    "FAMILIES",
                    "FAMILY_MEMBERS",
                    "BUDGETS",
                    "LOCATIONS",
                    "EXPENSE_ITEMS",
                    "GEOFENCE_ZONES",
                    "ALERTS"
            );

            for (String tableName : expectedTables) {
                ResultSet rs = conn.getMetaData().getTables(null, null, tableName, null);
                assertTrue(rs.next(), "Eroare: Tabelul " + tableName + " nu a fost gasit in schema!");
            }
        }
    }
}