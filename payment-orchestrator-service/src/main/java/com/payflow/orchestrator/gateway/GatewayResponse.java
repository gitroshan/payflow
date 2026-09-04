package com.payflow.orchestrator.gateway;

/**
 * Normalised response from any payment service provider adapter.
 *
 * @param approved       whether the PSP approved the operation
 * @param reference      the PSP-side transaction reference
 * @param declineReason  populated when {@code approved} is false
 */
public record GatewayResponse(boolean approved, String reference, String declineReason) {
}
