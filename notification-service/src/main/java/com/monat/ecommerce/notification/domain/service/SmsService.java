package com.monat.ecommerce.notification.domain.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class SmsService {

    private final SmsProvider smsProvider;

    public void sendSms(String phoneNumber, String message) {
        smsProvider.send(phoneNumber, message);
    }

    public void sendOrderConfirmationSms(String phoneNumber, String orderNumber) {
        sendSms(phoneNumber,
                String.format("Your order %s has been confirmed. Thank you for shopping with us!", orderNumber));
    }

    public void sendOrderCompletedSms(String phoneNumber, String orderNumber) {
        sendSms(phoneNumber,
                String.format("Great news! Your order %s is complete and will be shipped soon.", orderNumber));
    }

    public void sendOrderCancelledSms(String phoneNumber, String orderNumber) {
        sendSms(phoneNumber,
                String.format("Your order %s has been cancelled. Contact support if you have questions.", orderNumber));
    }
}
