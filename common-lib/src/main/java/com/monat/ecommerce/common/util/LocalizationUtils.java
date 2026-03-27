package com.monat.ecommerce.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

/**
 * Utility class for fetching localized messages.
 */
@Component
@RequiredArgsConstructor
public class LocalizationUtils {

    private final MessageSource messageSource;

    /**
     * Get a localized message for the given code.
     * @param code The message key
     * @return The localized message
     */
    public String getMessage(String code) {
        return messageSource.getMessage(code, null, LocaleContextHolder.getLocale());
    }

    /**
     * Get a localized message for the given code with arguments.
     * @param code The message key
     * @param args Arguments for the message
     * @return The localized message
     */
    public String getMessage(String code, Object... args) {
        return messageSource.getMessage(code, args, LocaleContextHolder.getLocale());
    }
}
