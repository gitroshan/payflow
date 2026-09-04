package com.payflow.gateway.api;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.math.BigDecimal;

/** Request/response contracts for the adapter API. */
public class GatewayDtos {

    public record AuthorizeRequest(
            @NotBlank String paymentId,
            @NotBlank String merchantId,
            @NotNull BigDecimal amount,
            @NotBlank String currency,
            @NotBlank String paymentMethodToken) {
    }

    public record CaptureRequest(@NotBlank String gatewayReference, @NotNull BigDecimal amount) {
    }

    public record RefundRequest(@NotBlank String gatewayReference, @NotNull BigDecimal amount) {
    }

    public record GatewayResponse(boolean approved, String reference, String declineReason) {
    }
}
