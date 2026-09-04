package com.payflow.orchestrator.gateway;

import java.math.BigDecimal;

public record GatewayAuthorizeRequest(
        String paymentId,
        String merchantId,
        BigDecimal amount,
        String currency,
        String paymentMethodToken) {
}
