package com.payflow.refund.repository;

import com.payflow.refund.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface Repositories {

    interface RefundRepository extends JpaRepository<Refund, String> {
        Optional<Refund> findByIdempotencyKey(String idempotencyKey);
        List<Refund> findByPaymentId(String paymentId);
    }

    interface DisputeRepository extends JpaRepository<Dispute, String> {
        List<Dispute> findByPaymentId(String paymentId);
    }

    interface PaymentSnapshotRepository extends JpaRepository<PaymentSnapshot, String> {
    }

    interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
        List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
    }

    interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    }
}
