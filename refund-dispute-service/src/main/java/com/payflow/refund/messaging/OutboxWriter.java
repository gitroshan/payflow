package com.payflow.refund.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.event.DomainEvent;
import com.payflow.refund.domain.OutboxEvent;
import com.payflow.refund.repository.Repositories.OutboxRepository;
import org.springframework.stereotype.Component;

@Component
public class OutboxWriter {

    private final OutboxRepository outbox;
    private final ObjectMapper mapper;

    public OutboxWriter(OutboxRepository outbox, ObjectMapper eventObjectMapper) {
        this.outbox = outbox;
        this.mapper = eventObjectMapper;
    }

    public void write(String topic, DomainEvent event) {
        try {
            OutboxEvent row = new OutboxEvent();
            row.setEventId(event.eventId());
            row.setAggregateId(event.aggregateId());
            row.setEventType(event.eventType());
            row.setTopic(topic);
            row.setPayload(mapper.writeValueAsString(event));
            outbox.save(row);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to serialise event", e);
        }
    }
}
