package com.monat.ecommerce.notification.infrastructure.config;

import com.monat.ecommerce.notification.domain.service.SmsProvider;
import com.monat.ecommerce.notification.infrastructure.sms.ConsoleSmsProvider;
import com.monat.ecommerce.notification.infrastructure.sms.TextBeltSmsProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Slf4j
@Configuration
public class SmsConfig {

    @Value("${application.notification.sms-provider:console}")
    private String smsProvider;

    @Value("${application.notification.textbelt.api-key:textbelt_test}")
    private String textbeltApiKey;

    @Bean
    public SmsProvider smsProvider() {
        if ("textbelt".equalsIgnoreCase(smsProvider)) {
            log.info("SMS provider: TextBelt (key={})", textbeltApiKey);
            return new TextBeltSmsProvider(textbeltApiKey);
        }
        log.info("SMS provider: console (simulate)");
        return new ConsoleSmsProvider();
    }
}
