package com.familie.cheltuieli_familie.security.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.mockito.Mockito;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class FirebaseNotificationServiceTest {

    private FirebaseNotificationService service;
    private MockedStatic<FirebaseMessaging> mockedFirebaseStatic;
    private FirebaseMessaging mockFirebaseMessaging;

    @BeforeEach
    void setUp() {
        service = new FirebaseNotificationService();

        // Mock-uim instanța de Firebase ca să nu trimită notificări reale către telefoane în timpul testelor
        mockFirebaseMessaging = mock(FirebaseMessaging.class);

        // "Păcălim" metoda statică getInstance() să returneze mock-ul nostru
        mockedFirebaseStatic = Mockito.mockStatic(FirebaseMessaging.class);
        mockedFirebaseStatic.when(FirebaseMessaging::getInstance).thenReturn(mockFirebaseMessaging);
    }

    @AfterEach
    void tearDown() {
        // Este obligatoriu să închidem mock-ul static după fiecare test pentru a nu afecta celelalte teste din proiect
        mockedFirebaseStatic.close();
    }

    @Test
    void testSendPushNotification_Success() throws Exception {
        // Simulam că Firebase funcționează perfect și returnează un ID de mesaj
        when(mockFirebaseMessaging.send(any(Message.class))).thenReturn("mesaj-id-123");

        service.sendPushNotification("token_test", "Alertă", "Mesaj de test");

        // Verificăm dacă metoda send() a fost apelată exact o dată
        Mockito.verify(mockFirebaseMessaging, Mockito.times(1)).send(any(Message.class));
    }

    @Test
    void testSendPushNotification_CatchException() throws Exception {
        // Simulăm o eroare în Firebase ca să forțăm intrarea pe ramura "catch"
        when(mockFirebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("Firebase error"));

        service.sendPushNotification("token_test", "Alertă", "Mesaj de test");

        // Verificăm că a încercat să trimită, dar a intrat elegant pe catch (fără să oprească aplicația)
        Mockito.verify(mockFirebaseMessaging, Mockito.times(1)).send(any(Message.class));
    }
}