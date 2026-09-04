package com.payflow.payout.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.KafkaTopics;
import com.payflow.common.event.PaymentCapturedEvent;
import com.payflow.payout.domain.PaymentSnapshot;
import com.payflow.payout.domain.ProcessedEvent;
import com.payflow.payout.repository.Repositories.PaymentSnapshotRepository;
import com.payflow.payout.repository.Repositories.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final PaymentSnapshotRepository snapshots;
    private final ProcessedEventRepository processed;
    private final ObjectMapper mapper;

    public PaymentEventConsumer(PaymentSnapshotRepository snapshots,
                                ProcessedEventRepository processed, ObjectMapper eventObjectMapper) {
        this.snapshots = snapshots;
        this.processed = processed;
        this.mapper = eventObjectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    @Transactional
    public void onPaymentEvent(@Payload String payload,
                               @Header(name = "eventType", required = false) String eventType) {
        if (!"PaymentCaptured".equals(eventType)) {
            return;
        }
        try {
            PaymentCapturedEvent e = mapper.readValue(payload, PaymentCapturedEvent.class);
            if (processed.existsById(e.eventId())) {
                return;
            }
            PaymentSnapshot snapshot = snapshots.findById(e.paymentId()).orElseGet(PaymentSnapshot::new);
            snapshot.setPaymentId(e.paymentId());
            snapshot.setMerchantId(e.merchantId());
            snapshot.setAmount(e.amount().amount());
            snapshot.setCurrency(e.amount().currency());
            snapshot.setGatewayReference(e.gatewayReference());
            snapshots.save(snapshot);
            processed.save(new ProcessedEvent(e.eventId()));
        } catch (Exception ex) {
            log.error("Failed to project payment event", ex);
            throw new RuntimeException(ex);
        }
    }
}
