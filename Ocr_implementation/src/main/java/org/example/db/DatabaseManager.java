package org.example.db;

public class DatabaseManager {

    public void connect() {
        System.out.println("Connecting to database");
    }

    public void disconnect() {
        System.out.println("Disconnecting from database");
    }

    public void executeInsert(String sql) {
        System.out.println("Executing SQL: " + sql);
    }
}