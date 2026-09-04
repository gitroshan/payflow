package com.payflow.orchestrator.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

/**
 * Transactional outbox row. Events are written in the SAME database transaction
 * that mutates the {@link Payment}; a background publisher then relays them to
 * Kafka. This guarantees that state changes and their events cannot diverge,
 * even if the broker is briefly unavailable (the classic dual-write problem).
 */
@Entity
@Table(name = "outbox_events", indexes = @Index(name = "ix_outbox_unpublished", columnList = "published, createdAt"))
@Getter
@Setter
@NoArgsConstructor
public class OutboxEvent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 36)
    private String eventId;

    @Column(nullable = false, length = 64)
    private String aggregateType;

    @Column(nullable = false, length = 36)
    private String aggregateId;

    @Column(nullable = false, length = 64)
    private String eventType;

    @Column(nullable = false, length = 64)
    private String topic;

    @Column(nullable = false, columnDefinition = "text")
    private String payload;

    @Column(nullable = false)
    private boolean published = false;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    private Instant publishedAt;
}
