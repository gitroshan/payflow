package com.payflow.payout.service;

import com.payflow.common.enums.ReconciliationStatus;
import com.payflow.payout.domain.PaymentSnapshot;
import com.payflow.payout.domain.ReconciliationRecord;
import com.payflow.payout.repository.Repositories.PaymentSnapshotRepository;
import com.payflow.payout.repository.Repositories.ReconciliationRecordRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;

/**
 * Three-way-style reconciliation between the provider's settlement report and
 * the platform's own captured-payment projection. Every provider line and every
 * internal captured payment is accounted for, so nothing silently goes missing.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);

    private final PaymentSnapshotRepository snapshots;
    private final ReconciliationRecordRepository records;

    public ReconciliationService(PaymentSnapshotRepository snapshots,
                                 ReconciliationRecordRepository records) {
        this.snapshots = snapshots;
        this.records = records;
    }

    /** One line of a provider settlement report. */
    public record ProviderLine(String paymentId, BigDecimal amount, String currency) {
    }

    public record ReconciliationSummary(String batchId, int matched, int amountMismatch,
                                        int missingInLedger, int missingInProvider,
                                        List<ReconciliationRecord> records) {
    }

    @Transactional
    public ReconciliationSummary reconcile(List<ProviderLine> providerLines) {
        String batchId = "recon_" + UUID.randomUUID().toString().substring(0, 8);
        Map<String, PaymentSnapshot> byId = new HashMap<>();
        snapshots.findAll().forEach(s -> byId.put(s.getPaymentId(), s));

        Set<String> seenInProvider = new HashSet<>();
        List<ReconciliationRecord> results = new ArrayList<>();
        int matched = 0, mismatch = 0, missingLedger = 0, missingProvider = 0;

        for (ProviderLine line : providerLines) {
            seenInProvider.add(line.paymentId());
            PaymentSnapshot snapshot = byId.get(line.paymentId());
            ReconciliationStatus status;
            BigDecimal internal = snapshot != null ? snapshot.getAmount() : null;
            if (snapshot == null) {
                status = ReconciliationStatus.MISSING_IN_LEDGER;
                missingLedger++;
            } else if (internal.compareTo(line.amount()) != 0) {
                status = ReconciliationStatus.AMOUNT_MISMATCH;
                mismatch++;
            } else {
                status = ReconciliationStatus.MATCHED;
                matched++;
            }
            results.add(record(batchId, line.paymentId(), internal, line.amount(), line.currency(), status));
        }

        for (PaymentSnapshot snapshot : byId.values()) {
            if (!seenInProvider.contains(snapshot.getPaymentId())) {
                results.add(record(batchId, snapshot.getPaymentId(), snapshot.getAmount(), null,
                        snapshot.getCurrency(), ReconciliationStatus.MISSING_IN_PROVIDER));
                missingProvider++;
            }
        }

        records.saveAll(results);
        log.info("Reconciliation {} done: matched={}, mismatch={}, missingLedger={}, missingProvider={}",
                batchId, matched, mismatch, missingLedger, missingProvider);
        return new ReconciliationSummary(batchId, matched, mismatch, missingLedger, missingProvider, results);
    }

    private ReconciliationRecord record(String batchId, String paymentId, BigDecimal internal,
                                        BigDecimal provider, String currency, ReconciliationStatus status) {
        ReconciliationRecord r = new ReconciliationRecord();
        r.setId(UUID.randomUUID().toString());
        r.setBatchId(batchId);
        r.setPaymentId(paymentId);
        r.setInternalAmount(internal);
        r.setProviderAmount(provider);
        r.setCurrency(currency);
        r.setStatus(status);
        r.setCreatedAt(Instant.now());
        return r;
    }

    @Transactional(readOnly = true)
    public List<ReconciliationRecord> batch(String batchId) {
        return records.findByBatchId(batchId);
    }
}
