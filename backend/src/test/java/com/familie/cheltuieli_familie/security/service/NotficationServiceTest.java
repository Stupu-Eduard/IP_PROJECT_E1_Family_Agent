package com.familie.cheltuieli_familie.security.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

class NotficationServiceTest {

    @Test
    void testSendAlert() {
        // Inițializăm serviciul
        NotficationService service = new NotficationService();

        // Verificăm pur și simplu că apelarea metodei funcționează și nu aruncă nicio eroare (excepție)
        assertDoesNotThrow(() -> service.sendAlert("Mesaj de test pentru acoperirea codului"));
    }
}