package com.familie.cheltuieli_familie.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotficationServiceTest {

    private final NotficationService service = new NotficationService();

    @Test
    void testSendAlert() {
        assertDoesNotThrow(() -> service.sendAlert("Test alert"));
    }

    @Test
    void testSendAlertWithEmptyMessage() {
        assertDoesNotThrow(() -> service.sendAlert(""));
    }

    @Test
    void testSendAlertWithSpecialCharacters() {
        assertDoesNotThrow(() -> service.sendAlert("Alert! @#$% Special chars ăîâșț"));
    }
}
