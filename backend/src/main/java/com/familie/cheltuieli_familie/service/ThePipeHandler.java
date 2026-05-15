package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
@Slf4j
public class ThePipeHandler extends TextWebSocketHandler {

    private final ConcurrentHashMap<Long, List<WebSocketSession>> sessionsByFamily = new ConcurrentHashMap<>();

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        Long familyId = (Long) session.getAttributes().get("familyId");
        if (familyId == null) {
            log.warn("⚠️ THE PIPE: Sesiune fără familyId în atribute, ignorată. ID: {}", session.getId());
            return;
        }
        sessionsByFamily.computeIfAbsent(familyId, k -> new CopyOnWriteArrayList<>()).add(session);
        log.info("🟢 THE PIPE: Conexiune nouă stabilită! ID: {}, familyId: {}", session.getId(), familyId);
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Long familyId = (Long) session.getAttributes().get("familyId");
        if (familyId != null) {
            List<WebSocketSession> list = sessionsByFamily.get(familyId);
            if (list != null) {
                list.remove(session);
                if (list.isEmpty()) {
                    sessionsByFamily.remove(familyId);
                }
            }
        }
        log.info("🔴 THE PIPE: Conexiune închisă. ID: {}, Status: {}", session.getId(), status);
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        log.info("📩 THE PIPE: Mesaj primit: {}", message.getPayload());
    }

    public void sendToParent(Long familyId, String payload) {
        List<WebSocketSession> sessions = sessionsByFamily.get(familyId);
        if (sessions == null || sessions.isEmpty()) {
            log.debug("📡 THE PIPE: Niciun client conectat pentru familyId={}", familyId);
            return;
        }
        log.info("📡 THE PIPE: Se trimit date către familyId={} ({} sesiuni)", familyId, sessions.size());
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

    public void broadcast(String payload) {
        int total = sessionsByFamily.values().stream().mapToInt(List::size).sum();
        log.info("📡 THE PIPE: Broadcast general către {} clienți", total);
        for (List<WebSocketSession> sessions : sessionsByFamily.values()) {
            for (WebSocketSession session : sessions) {
                if (session.isOpen()) {
                    try {
                        session.sendMessage(new TextMessage(payload));
                    } catch (IOException e) {
                        log.error("❌ THE PIPE: Eroare broadcast sesiunea {}", session.getId(), e);
                    }
                }
            }
        }
    }
}
