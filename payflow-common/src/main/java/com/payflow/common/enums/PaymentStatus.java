package com.payflow.common.enums;

/** Lifecycle of a payment as tracked by the orchestrator saga. */
public enum PaymentStatus {
    CREATED,
    AUTHORIZING,
    AUTHORIZED,
    CAPTURING,
    CAPTURED,
    SETTLED,
    FAILED,
    VOIDED,
    REFUNDED,
    PARTIALLY_REFUNDED
}
