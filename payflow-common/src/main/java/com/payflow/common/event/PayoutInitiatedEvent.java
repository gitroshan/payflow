package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record PayoutInitiatedEvent(
        String eventId,
        String payoutId,
        String merchantId,
        Money amount,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "PayoutInitiated"; }
    public String aggregateId() { return payoutId; }
}
