package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Handler-ul principal pentru THE PIPE.
 * Gestionează conexiunile WebSocket active și trimite datele în timp real către toți clienții (părinții).
 */
@Component
@Slf4j
public class ThePipeHandler extends TextWebSocketHandler {

    private final List<WebSocketSession> sessions = new CopyOnWriteArrayList<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("🟢 THE PIPE: Conexiune nouă stabilită! ID: {}", session.getId());
        sessions.add(session);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        log.info("🔴 THE PIPE: Conexiune închisă. ID: {}, Status: {}", session.getId(), status);
        sessions.remove(session);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("📩 THE PIPE: Mesaj primit: {}", message.getPayload());
    }

    public void broadcast(String payload) {
        log.info("📡 THE PIPE: Se trimit date către {} clienți: {}", sessions.size(), payload);
        for (WebSocketSession session : sessions) {
            if (session.isOpen()) {
                try {
                    session.sendMessage(new TextMessage(payload));
                } catch (IOException e) {
                    log.error("❌ THE PIPE: Eroare la trimiterea mesajului către sesiunea {}", session.getId(), e);
                }
            }
        }
    }
}
