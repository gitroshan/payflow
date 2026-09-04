package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record DisputeOpenedEvent(
        String eventId,
        String disputeId,
        String paymentId,
        String merchantId,
        Money amount,
        String reasonCode,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "DisputeOpened"; }
    public String aggregateId() { return disputeId; }
}
