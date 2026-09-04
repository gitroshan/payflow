package com.payflow.payout.repository;

import com.payflow.payout.domain.*;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface Repositories {

    interface PayoutRepository extends JpaRepository<Payout, String> {
        List<Payout> findByMerchantId(String merchantId);
    }

    interface ReconciliationRecordRepository extends JpaRepository<ReconciliationRecord, String> {
        List<ReconciliationRecord> findByBatchId(String batchId);
    }

    interface PaymentSnapshotRepository extends JpaRepository<PaymentSnapshot, String> {
    }

    interface ProcessedEventRepository extends JpaRepository<ProcessedEvent, String> {
    }

    interface OutboxRepository extends JpaRepository<OutboxEvent, Long> {
        List<OutboxEvent> findByPublishedFalseOrderByCreatedAtAsc(Pageable pageable);
    }
}
