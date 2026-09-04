package com.payflow.refund.service;

import com.payflow.common.KafkaTopics;
import com.payflow.common.enums.DisputeStatus;
import com.payflow.common.event.DisputeOpenedEvent;
import com.payflow.common.money.Money;
import com.payflow.refund.domain.Dispute;
import com.payflow.refund.domain.PaymentSnapshot;
import com.payflow.refund.messaging.OutboxWriter;
import com.payflow.refund.repository.Repositories.DisputeRepository;
import com.payflow.refund.repository.Repositories.PaymentSnapshotRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Service
public class DisputeService {

    private final DisputeRepository disputes;
    private final PaymentSnapshotRepository snapshots;
    private final OutboxWriter outbox;

    public DisputeService(DisputeRepository disputes, PaymentSnapshotRepository snapshots,
                          OutboxWriter outbox) {
        this.disputes = disputes;
        this.snapshots = snapshots;
        this.outbox = outbox;
    }

    @Transactional
    public Dispute open(String paymentId, String reasonCode) {
        PaymentSnapshot snapshot = snapshots.findById(paymentId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND,
                        "No captured payment known for " + paymentId));
        Dispute dispute = new Dispute();
        dispute.setId(UUID.randomUUID().toString());
        dispute.setPaymentId(paymentId);
        dispute.setMerchantId(snapshot.getMerchantId());
        dispute.setAmount(snapshot.getCapturedAmount());
        dispute.setCurrency(snapshot.getCurrency());
        dispute.setReasonCode(reasonCode);
        dispute.setStatus(DisputeStatus.OPEN);
        disputes.save(dispute);

        outbox.write(KafkaTopics.DISPUTE_EVENTS, new DisputeOpenedEvent(
                UUID.randomUUID().toString(), dispute.getId(), paymentId, snapshot.getMerchantId(),
                new Money(snapshot.getCapturedAmount(), snapshot.getCurrency()), reasonCode, Instant.now()));
        return dispute;
    }

    @Transactional
    public Dispute transition(String disputeId, DisputeStatus newStatus) {
        Dispute dispute = disputes.findById(disputeId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Dispute not found"));
        dispute.setStatus(newStatus);
        if (newStatus == DisputeStatus.WON || newStatus == DisputeStatus.LOST) {
            dispute.setResolvedAt(Instant.now());
        }
        return dispute;
    }

    @Transactional(readOnly = true)
    public List<Dispute> forPayment(String paymentId) {
        return disputes.findByPaymentId(paymentId);
    }
}
