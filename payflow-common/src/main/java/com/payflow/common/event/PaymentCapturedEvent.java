package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record PaymentCapturedEvent(
        String eventId,
        String paymentId,
        String merchantId,
        Money amount,
        String gatewayReference,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "PaymentCaptured"; }
    public String aggregateId() { return paymentId; }
}
