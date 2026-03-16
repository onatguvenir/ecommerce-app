package com.monat.ecommerce.notification.domain.service;

import com.monat.ecommerce.events.notification.NotificationType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

/**
 * SMS notification service (simulated)
 */
@Slf4j
@Service
public class SmsService {

    /**
     * Send SMS notification (simulated)
     */
    public void sendSms(String phoneNumber, String message) {
        String logMessage = String.format(
                "%n%s%n📱 %s NOTIFICATION%nTo: %s%nMessage: %s%n%s",
                "=".repeat(80),
                NotificationType.SMS,
                phoneNumber,
                message,
                "=".repeat(80)
        );
        log.info(logMessage);
    }

    /**
     * Send order confirmation SMS
     */
    public void sendOrderConfirmationSms(String phoneNumber, String orderNumber) {
        String message = String.format(
                "Your order %s has been confirmed. Thank you for shopping with us!",
                orderNumber
        );
        sendSms(phoneNumber, message);
    }

    /**
     * Send order completed SMS
     */
    public void sendOrderCompletedSms(String phoneNumber, String orderNumber) {
        String message = String.format(
                "Great news! Your order %s is complete and will be shipped soon.",
                orderNumber
        );
        sendSms(phoneNumber, message);
    }

    /**
     * Send order cancelled SMS
     */
    public void sendOrderCancelledSms(String phoneNumber, String orderNumber) {
        String message = String.format(
                "Your order %s has been cancelled. Contact support if you have questions.",
                orderNumber
        );
        sendSms(phoneNumber, message);
    }
}
