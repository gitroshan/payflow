package com.payflow.orchestrator.messaging;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.event.DomainEvent;
import com.payflow.orchestrator.domain.OutboxEvent;
import com.payflow.orchestrator.repository.OutboxRepository;
import org.springframework.stereotype.Component;

/**
 * Persists domain events into the transactional outbox. MUST be called inside
 * the same @Transactional method that mutates the aggregate.
 */
@Component
public class OutboxWriter {

    private final OutboxRepository outboxRepository;
    private final ObjectMapper objectMapper;

    public OutboxWriter(OutboxRepository outboxRepository, ObjectMapper objectMapper) {
        this.outboxRepository = outboxRepository;
        this.objectMapper = objectMapper;
    }

    public void write(String aggregateType, String topic, DomainEvent event) {
        OutboxEvent row = new OutboxEvent();
        row.setEventId(event.eventId());
        row.setAggregateType(aggregateType);
        row.setAggregateId(event.aggregateId());
        row.setEventType(event.eventType());
        row.setTopic(topic);
        row.setPayload(serialize(event));
        outboxRepository.save(row);
    }

    private String serialize(DomainEvent event) {
        try {
            return objectMapper.writeValueAsString(event);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("Failed to serialise event " + event.eventType(), e);
        }
    }
}
