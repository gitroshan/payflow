package com.payflow.common;

/** Canonical Kafka topic names shared across every service. */
public final class KafkaTopics {

    private KafkaTopics() {
    }

    public static final String PAYMENT_EVENTS = "payment.events";
    public static final String REFUND_EVENTS = "refund.events";
    public static final String DISPUTE_EVENTS = "dispute.events";
    public static final String PAYOUT_EVENTS = "payout.events";
    public static final String LEDGER_EVENTS = "ledger.events";
}
