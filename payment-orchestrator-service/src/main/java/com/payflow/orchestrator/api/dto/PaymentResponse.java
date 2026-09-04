package com.payflow.orchestrator.api.dto;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.orchestrator.domain.Payment;

import java.math.BigDecimal;
import java.time.Instant;

public record PaymentResponse(
        String id,
        String merchantId,
        BigDecimal amount,
        String currency,
        BigDecimal capturedAmount,
        BigDecimal refundedAmount,
        PaymentStatus status,
        String gatewayReference,
        String failureReason,
        Instant createdAt,
        Instant updatedAt) {

    public static PaymentResponse from(Payment p) {
        return new PaymentResponse(
                p.getId(), p.getMerchantId(), p.getAmount(), p.getCurrency(),
                p.getCapturedAmount(), p.getRefundedAmount(), p.getStatus(),
                p.getGatewayReference(), p.getFailureReason(),
                p.getCreatedAt(), p.getUpdatedAt());
    }
}
