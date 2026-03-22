package com.monat.ecommerce.events.fraud;

import java.time.Instant;
import java.util.UUID;

/**
 * Event published when a user's account is suspended due to fraud detection.
 */
public record UserAccountSuspendedEvent(
        UUID eventId,
        String userId,
        String reason,
        Instant timestamp
) {
    public UserAccountSuspendedEvent {
        if (eventId == null) eventId = UUID.randomUUID();
        if (timestamp == null) timestamp = Instant.now();
    }
    
    public static UserAccountSuspendedEvent create(String userId, String reason) {
        return new UserAccountSuspendedEvent(UUID.randomUUID(), userId, reason, Instant.now());
    }
}
