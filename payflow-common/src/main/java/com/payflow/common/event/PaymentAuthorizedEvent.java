package com.payflow.common.event;

import com.payflow.common.money.Money;
import java.time.Instant;

public record PaymentAuthorizedEvent(
        String eventId,
        String paymentId,
        String merchantId,
        Money amount,
        String gatewayReference,
        Instant occurredAt) implements DomainEvent {

    public String eventType() { return "PaymentAuthorized"; }
    public String aggregateId() { return paymentId; }
}
