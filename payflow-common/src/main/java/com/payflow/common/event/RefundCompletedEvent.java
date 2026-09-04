package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record RefundCompletedEvent(
        String eventId,
        String refundId,
        String paymentId,
        String merchantId,
        Money amount,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "RefundCompleted"; }
    public String aggregateId() { return refundId; }
}
