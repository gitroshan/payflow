package com.payflow.orchestrator.api.dto;

import jakarta.validation.constraints.DecimalMin;

import java.math.BigDecimal;

/** Optional partial-capture amount; null captures the full authorized amount. */
public record CaptureRequest(@DecimalMin(value = "0.01") BigDecimal amount) {
}
