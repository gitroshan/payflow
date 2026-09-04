package com.payflow.orchestrator.gateway;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

/**
 * Synchronous client to the gateway-adapter-service, which fronts the real PSPs.
 * Authorization/capture are synchronous because the caller needs an immediate
 * approve/decline decision; everything downstream of capture is asynchronous.
 */
@Component
public class GatewayClient {

    private static final Logger log = LoggerFactory.getLogger(GatewayClient.class);
    private final RestClient restClient;

    public GatewayClient(RestClient gatewayRestClient) {
        this.restClient = gatewayRestClient;
    }

    public GatewayResponse authorize(GatewayAuthorizeRequest request) {
        log.info("Authorizing payment {} via gateway", request.paymentId());
        return restClient.post()
                .uri("/gateway/authorize")
                .body(request)
                .retrieve()
                .body(GatewayResponse.class);
    }

    public GatewayResponse capture(GatewayCaptureRequest request) {
        log.info("Capturing gateway reference {}", request.gatewayReference());
        return restClient.post()
                .uri("/gateway/capture")
                .body(request)
                .retrieve()
                .body(GatewayResponse.class);
    }
}
