package com.familie.cheltuieli_familie.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class NotficationService {

    public void sendAlert(String message) {
        log.info("[SMS/FIREBASE ALERT]: {}", message);
    }
}