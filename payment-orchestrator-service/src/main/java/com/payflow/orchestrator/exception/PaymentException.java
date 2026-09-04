package com.payflow.orchestrator.exception;

import org.springframework.http.HttpStatus;

public class PaymentException extends RuntimeException {
    private final HttpStatus status;

    public PaymentException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }

    public HttpStatus getStatus() {
        return status;
    }

    public static PaymentException notFound(String id) {
        return new PaymentException(HttpStatus.NOT_FOUND, "Payment not found: " + id);
    }

    public static PaymentException conflict(String message) {
        return new PaymentException(HttpStatus.CONFLICT, message);
    }
}
