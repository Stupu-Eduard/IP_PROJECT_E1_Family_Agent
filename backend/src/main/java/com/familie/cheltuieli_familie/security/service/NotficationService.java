package com.familie.cheltuieli_familie.security.service;

import org.springframework.stereotype.Service;

@Service
public class NotficationService {

    public void sendAlert(String message) {
        System.out.println("!!! [SMS/FIREBASE ALERT]: " + message);
    }
}