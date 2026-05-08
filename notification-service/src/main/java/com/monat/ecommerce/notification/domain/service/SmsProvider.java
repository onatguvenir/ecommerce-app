package com.monat.ecommerce.notification.domain.service;

public interface SmsProvider {
    void send(String to, String message);
}
