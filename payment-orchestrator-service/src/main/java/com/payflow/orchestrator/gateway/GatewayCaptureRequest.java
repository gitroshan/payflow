package com.payflow.orchestrator.gateway;

import java.math.BigDecimal;

public record GatewayCaptureRequest(String gatewayReference, BigDecimal amount) {
}
