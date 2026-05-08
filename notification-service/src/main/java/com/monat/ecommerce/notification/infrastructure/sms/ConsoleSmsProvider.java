package com.monat.ecommerce.notification.infrastructure.sms;

import com.monat.ecommerce.notification.domain.service.SmsProvider;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class ConsoleSmsProvider implements SmsProvider {

    @Override
    public void send(String to, String message) {
        log.info("\n{}\n📱 SMS (console)\nTo: {}\nMessage: {}\n{}",
                "=".repeat(80), to, message, "=".repeat(80));
    }
}
