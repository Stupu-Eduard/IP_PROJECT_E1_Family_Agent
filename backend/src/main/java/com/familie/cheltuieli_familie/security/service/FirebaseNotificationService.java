package com.familie.cheltuieli_familie.security.service;

import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.Notification;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

@Service
public class FirebaseNotificationService {

    // 1. Adăugăm Logger-ul oficial pentru Spring Boot
    private static final Logger logger = LoggerFactory.getLogger(FirebaseNotificationService.class);

    public void sendPushNotification(String token, String title, String body) {
        try {
            Message message = Message.builder()
                    .setToken(token)
                    .setNotification(Notification.builder()
                            .setTitle(title)
                            .setBody(body)
                            .build())
                    .build();

            String response = FirebaseMessaging.getInstance().send(message);

            // 2. Folosim logger.info în loc de System.out
            logger.info("Notificare trimisa cu succes către {}: {}", token, response);

        } catch (Exception e) {
            // 3. Folosim logger.error și AFIȘĂM eroarea reală (e.getMessage())
            logger.error("Eroare la trimiterea notificării Firebase: {}", e.getMessage(), e);
        }
    }
}