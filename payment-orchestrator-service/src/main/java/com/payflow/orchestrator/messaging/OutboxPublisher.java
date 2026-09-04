package com.payflow.orchestrator.messaging;

import com.payflow.orchestrator.domain.OutboxEvent;
import com.payflow.orchestrator.repository.OutboxRepository;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.header.internals.RecordHeader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.PageRequest;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.List;

/**
 * Polls the outbox and relays unpublished events to Kafka (at-least-once).
 * Consumers must therefore be idempotent. Runs on a short fixed delay; in a
 * real deployment this would use Debezium CDC or a leader-elected poller.
 */
@Component
public class OutboxPublisher {

    private static final Logger log = LoggerFactory.getLogger(OutboxPublisher.class);
    private final OutboxRepository outboxRepository;
    private final KafkaTemplate<String, String> kafkaTemplate;

    public OutboxPublisher(OutboxRepository outboxRepository, KafkaTemplate<String, String> kafkaTemplate) {
        this.outboxRepository = outboxRepository;
        this.kafkaTemplate = kafkaTemplate;
    }

    @Scheduled(fixedDelayString = "${payflow.outbox.poll-interval-ms:1000}")
    @Transactional
    public void publishPending() {
        List<OutboxEvent> batch =
                outboxRepository.findByPublishedFalseOrderByCreatedAtAsc(PageRequest.of(0, 100));
        if (batch.isEmpty()) {
            return;
        }
        for (OutboxEvent event : batch) {
            try {
                // Keyed by aggregateId to preserve per-payment ordering across partitions.
                ProducerRecord<String, String> record = new ProducerRecord<>(
                        event.getTopic(), event.getAggregateId(), event.getPayload());
                record.headers().add(new RecordHeader("eventType",
                        event.getEventType().getBytes(StandardCharsets.UTF_8)));
                record.headers().add(new RecordHeader("eventId",
                        event.getEventId().getBytes(StandardCharsets.UTF_8)));
                kafkaTemplate.send(record).get();
                event.setPublished(true);
                event.setPublishedAt(Instant.now());
            } catch (Exception e) {
                log.error("Failed to publish outbox event {} - will retry next cycle",
                        event.getEventId(), e);
                // leave unpublished; next poll retries
            }
        }
    }
}
