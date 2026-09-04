package com.payflow.common.event;

import java.time.Instant;

/**
 * Marker contract for every event published to Kafka. Carries the correlation
 * fields needed for tracing and idempotent consumption on the receiving side.
 */
public interface DomainEvent {
    String eventId();
    String eventType();
    String aggregateId();
    Instant occurredAt();
}
