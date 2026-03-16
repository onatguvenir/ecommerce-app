package com.monat.ecommerce.events.notification;

import com.monat.ecommerce.events.BaseEvent;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * Generic notification event
 */
@Data
@Builder
@EqualsAndHashCode(callSuper = true)
@NoArgsConstructor
@AllArgsConstructor
public class NotificationEvent extends BaseEvent {
    private String recipientEmail;
    private String recipientPhone;
    private NotificationType type;
    private String subject;
    private String body;
    private Map<String, Object> templateVariables;
}
