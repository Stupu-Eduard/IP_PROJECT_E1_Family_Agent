package org.example.db;

public class DatabaseManager {

    public void connect() {
        System.out.println("Connecting to storage system...");
    }

    public void disconnect() {
        System.out.println("Disconnecting from storage system...");
    }

    public void send(String payload) {
        System.out.println("Sending payload: " + payload);
    }
}