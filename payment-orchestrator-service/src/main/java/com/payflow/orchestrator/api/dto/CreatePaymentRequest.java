package com.payflow.orchestrator.api.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

/**
 * Request to create and authorize a payment.
 *
 * @param captureImmediately when true the orchestrator performs an auth+capture
 *                           in one flow (sale); when false it only authorizes and
 *                           waits for an explicit capture call.
 */
public record CreatePaymentRequest(
        @NotBlank String merchantId,
        @NotNull @DecimalMin(value = "0.01") BigDecimal amount,
        @NotBlank @Size(min = 3, max = 3) String currency,
        @NotBlank String paymentMethodToken,
        boolean captureImmediately) {
}
