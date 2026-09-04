package com.payflow.refund.service;

import com.payflow.common.KafkaTopics;
import com.payflow.common.enums.RefundStatus;
import com.payflow.common.event.RefundCompletedEvent;
import com.payflow.common.money.Money;
import com.payflow.refund.domain.PaymentSnapshot;
import com.payflow.refund.domain.Refund;
import com.payflow.refund.gateway.GatewayClient;
import com.payflow.refund.messaging.OutboxWriter;
import com.payflow.refund.repository.Repositories.PaymentSnapshotRepository;
import com.payflow.refund.repository.Repositories.RefundRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class RefundService {

    private static final Logger log = LoggerFactory.getLogger(RefundService.class);

    private final RefundRepository refunds;
    private final PaymentSnapshotRepository snapshots;
    private final GatewayClient gateway;
    private final OutboxWriter outbox;

    public RefundService(RefundRepository refunds, PaymentSnapshotRepository snapshots,
                         GatewayClient gateway, OutboxWriter outbox) {
        this.refunds = refunds;
        this.snapshots = snapshots;
        this.gateway = gateway;
        this.outbox = outbox;
    }

    /**
     * Requests a (possibly partial) refund. Idempotent on the supplied key.
     * Validates against this service's own projection of the captured payment,
     * so it never has to call the orchestrator synchronously.
     */
    @Transactional
    public Refund refund(String idempotencyKey, String paymentId, BigDecimal amount, String reason) {
        var existing = refunds.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            return existing.get();
        }

        PaymentSnapshot snapshot = snapshots.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No captured payment known for " + paymentId));

        BigDecimal refundAmount = amount != null ? amount : snapshot.refundableAmount();
        if (refundAmount.compareTo(snapshot.refundableAmount()) > 0) {
            throw new ResponseStatusException(HttpStatus.CONFLICT,
                    "Refund amount exceeds refundable balance of " + snapshot.refundableAmount());
        }

        Refund refund = new Refund();
        refund.setId(UUID.randomUUID().toString());
        refund.setPaymentId(paymentId);
        refund.setMerchantId(snapshot.getMerchantId());
        refund.setAmount(refundAmount);
        refund.setCurrency(snapshot.getCurrency());
        refund.setReason(reason);
        refund.setIdempotencyKey(idempotencyKey);
        refund.setStatus(RefundStatus.PROCESSING);
        refunds.save(refund);

        try {
            var resp = gateway.refund(snapshot.getGatewayReference(), refundAmount);
            if (!resp.approved()) {
                refund.setStatus(RefundStatus.FAILED);
                return refund;
            }
        } catch (Exception e) {
            refund.setStatus(RefundStatus.FAILED);
            log.error("Gateway refund failed for {}", paymentId, e);
            return refund;
        }

        refund.setStatus(RefundStatus.COMPLETED);
        snapshot.setRefundedAmount(snapshot.getRefundedAmount().add(refundAmount));
        snapshots.save(snapshot);

        outbox.write(KafkaTopics.REFUND_EVENTS, new RefundCompletedEvent(
                UUID.randomUUID().toString(), refund.getId(), paymentId, snapshot.getMerchantId(),
                new Money(refundAmount, snapshot.getCurrency()), Instant.now()));

        log.info("Refund {} completed for payment {} amount {}", refund.getId(), paymentId, refundAmount);
        return refund;
    }

    @Transactional(readOnly = true)
    public List<Refund> forPayment(String paymentId) {
        return refunds.findByPaymentId(paymentId);
    }
}
