package com.monat.ecommerce.events;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Base class for all domain events
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public abstract class BaseEvent {
    private String eventId = UUID.randomUUID().toString();
    private LocalDateTime timestamp = LocalDateTime.now();
    private String aggregateId;
    private String aggregateType;
    private Integer version;
}
