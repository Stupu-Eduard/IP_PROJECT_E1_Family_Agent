package com.familie.cheltuieli_familie.service;

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
        mockFirebaseMessaging = mock(FirebaseMessaging.class);
        mockedFirebaseStatic = Mockito.mockStatic(FirebaseMessaging.class);
        mockedFirebaseStatic.when(FirebaseMessaging::getInstance).thenReturn(mockFirebaseMessaging);
    }

    @AfterEach
    void tearDown() {
        mockedFirebaseStatic.close();
    }

    @Test
    void testSendPushNotification_Success() throws Exception {
        when(mockFirebaseMessaging.send(any(Message.class))).thenReturn("mesaj-id-123");

        service.sendPushNotification("token_test", "Alertă", "Mesaj de test");

        Mockito.verify(mockFirebaseMessaging, Mockito.times(1)).send(any(Message.class));
    }

    @Test
    void testSendPushNotification_CatchException() throws Exception {
        when(mockFirebaseMessaging.send(any(Message.class))).thenThrow(new RuntimeException("Firebase error"));

        service.sendPushNotification("token_test", "Alertă", "Mesaj de test");

        Mockito.verify(mockFirebaseMessaging, Mockito.times(1)).send(any(Message.class));
    }
}