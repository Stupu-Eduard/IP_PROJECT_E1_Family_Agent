package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class MockNotificationServiceTest {

    private final MockNotificationService service = new MockNotificationService();

    @Test
    void testSendNotification() {
        assertDoesNotThrow(() -> service.sendNotification("Test alert message"));
    }

    @Test
    void testSendNotificationWithEmptyMessage() {
        assertDoesNotThrow(() -> service.sendNotification(""));
    }

    @Test
    void testSendNotificationWithLongMessage() {
        assertDoesNotThrow(() -> service.sendNotification("A".repeat(500)));
    }
}
