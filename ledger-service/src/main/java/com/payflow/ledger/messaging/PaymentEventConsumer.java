package com.payflow.ledger.messaging;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.payflow.common.KafkaTopics;
import com.payflow.common.event.PaymentCapturedEvent;
import com.payflow.common.event.RefundCompletedEvent;
import com.payflow.ledger.service.LedgerService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.support.KafkaHeaders;
import org.springframework.messaging.handler.annotation.Header;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.stereotype.Component;

/**
 * Consumes payment and refund events and projects them into the ledger.
 * Dispatch is driven by the {@code eventType} header the producer stamps on
 * each record, so the consumer never has to guess the payload shape.
 */
@Component
public class PaymentEventConsumer {

    private static final Logger log = LoggerFactory.getLogger(PaymentEventConsumer.class);

    private final LedgerService ledger;
    private final ObjectMapper mapper;

    public PaymentEventConsumer(LedgerService ledger, ObjectMapper eventObjectMapper) {
        this.ledger = ledger;
        this.mapper = eventObjectMapper;
    }

    @KafkaListener(topics = KafkaTopics.PAYMENT_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onPaymentEvent(@Payload String payload,
                               @Header(name = "eventType", required = false) String eventType) {
        try {
            if ("PaymentCaptured".equals(eventType)) {
                PaymentCapturedEvent e = mapper.readValue(payload, PaymentCapturedEvent.class);
                ledger.recordCapture(e.eventId(), e.paymentId(), e.merchantId(),
                        e.amount().amount(), e.amount().currency());
            } else {
                log.debug("Ignoring payment event of type {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process payment event ({})", eventType, ex);
            throw new RuntimeException(ex); // let the container retry / DLT
        }
    }

    @KafkaListener(topics = KafkaTopics.REFUND_EVENTS, groupId = "${spring.kafka.consumer.group-id}")
    public void onRefundEvent(@Payload String payload,
                              @Header(name = "eventType", required = false) String eventType) {
        try {
            if ("RefundCompleted".equals(eventType)) {
                RefundCompletedEvent e = mapper.readValue(payload, RefundCompletedEvent.class);
                ledger.recordRefund(e.eventId(), e.refundId(), e.paymentId(), e.merchantId(),
                        e.amount().amount(), e.amount().currency());
            } else {
                log.debug("Ignoring refund event of type {}", eventType);
            }
        } catch (Exception ex) {
            log.error("Failed to process refund event ({})", eventType, ex);
            throw new RuntimeException(ex);
        }
    }
}
