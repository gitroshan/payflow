package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record PaymentFailedEvent(
        String eventId,
        String paymentId,
        String merchantId,
        Money amount,
        String reason,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "PaymentFailed"; }
    public String aggregateId() { return paymentId; }
}
