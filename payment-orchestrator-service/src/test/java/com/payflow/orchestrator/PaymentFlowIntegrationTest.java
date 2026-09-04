package com.payflow.orchestrator;

import com.payflow.orchestrator.api.dto.PaymentResponse;
import com.payflow.orchestrator.gateway.GatewayClient;
import com.payflow.orchestrator.gateway.GatewayResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.test.web.client.TestRestTemplate;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.http.*;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.GenericContainer;
import org.testcontainers.containers.KafkaContainer;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

/**
 * End-to-end test of the core payment flow against real Postgres, Redis and Kafka
 * (via Testcontainers). The external PSP is stubbed so the test is hermetic.
 * Requires a running Docker daemon.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@Testcontainers
class PaymentFlowIntegrationTest {

    @Container
    @ServiceConnection
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16");

    @Container
    @ServiceConnection
    static KafkaContainer kafka =
            new KafkaContainer(DockerImageName.parse("confluentinc/cp-kafka:7.6.0"));

    @Container
    static GenericContainer<?> redis =
            new GenericContainer<>(DockerImageName.parse("redis:7")).withExposedPorts(6379);

    @DynamicPropertySource
    static void properties(DynamicPropertyRegistry registry) {
        registry.add("spring.data.redis.host", redis::getHost);
        registry.add("spring.data.redis.port", () -> redis.getMappedPort(6379));
        // Our KafkaProducerConfig reads this property directly.
        registry.add("spring.kafka.bootstrap-servers", kafka::getBootstrapServers);
    }

    @Autowired
    TestRestTemplate rest;

    @MockBean
    GatewayClient gatewayClient;

    @Test
    void createAndCaptureThenReplayIsIdempotent() {
        when(gatewayClient.authorize(any())).thenReturn(new GatewayResponse(true, "psp_ref_123", null));
        when(gatewayClient.capture(any())).thenReturn(new GatewayResponse(true, "psp_ref_123", null));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "order-42");
        String body = """
                {"merchantId":"merchant_acme","amount":49.99,"currency":"USD",
                 "paymentMethodToken":"tok_visa","captureImmediately":true}
                """;

        ResponseEntity<PaymentResponse> first =
                rest.postForEntity("/api/v1/payments", new HttpEntity<>(body, headers), PaymentResponse.class);
        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(first.getBody()).isNotNull();
        assertThat(first.getBody().status().name()).isEqualTo("CAPTURED");
        assertThat(first.getBody().capturedAmount()).isEqualByComparingTo("49.99");
        String paymentId = first.getBody().id();

        // Same idempotency key -> same payment, no second charge.
        ResponseEntity<PaymentResponse> replay =
                rest.postForEntity("/api/v1/payments", new HttpEntity<>(body, headers), PaymentResponse.class);
        assertThat(replay.getBody()).isNotNull();
        assertThat(replay.getBody().id()).isEqualTo(paymentId);

        // And it is persisted / retrievable.
        ResponseEntity<PaymentResponse> fetched =
                rest.getForEntity("/api/v1/payments/" + paymentId, PaymentResponse.class);
        assertThat(fetched.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(fetched.getBody().status().name()).isEqualTo("CAPTURED");
    }

    @Test
    void declinedAuthorizationMarksPaymentFailed() {
        when(gatewayClient.authorize(any()))
                .thenReturn(new GatewayResponse(false, null, "card_declined"));

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("Idempotency-Key", "order-decline-1");
        String body = """
                {"merchantId":"merchant_acme","amount":20.00,"currency":"USD",
                 "paymentMethodToken":"tok_declined","captureImmediately":true}
                """;

        ResponseEntity<PaymentResponse> resp =
                rest.postForEntity("/api/v1/payments", new HttpEntity<>(body, headers), PaymentResponse.class);
        assertThat(resp.getBody()).isNotNull();
        assertThat(resp.getBody().status().name()).isEqualTo("FAILED");
        assertThat(resp.getBody().failureReason()).isEqualTo("card_declined");
    }
}
