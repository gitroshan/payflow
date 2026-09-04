package com.payflow.refund.gateway;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;

@Component
public class GatewayClient {

    private final RestClient restClient;

    public GatewayClient(@Value("${payflow.gateway.base-url}") String baseUrl) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
    }

    public record RefundRequest(String gatewayReference, BigDecimal amount) {
    }

    public record GatewayResponse(boolean approved, String reference, String declineReason) {
    }

    public GatewayResponse refund(String gatewayReference, BigDecimal amount) {
        return restClient.post()
                .uri("/gateway/refund")
                .body(new RefundRequest(gatewayReference, amount))
                .retrieve()
                .body(GatewayResponse.class);
    }
}
