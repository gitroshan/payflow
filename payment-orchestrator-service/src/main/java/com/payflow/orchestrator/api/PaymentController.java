package com.payflow.orchestrator.api;

import com.payflow.orchestrator.api.dto.CaptureRequest;
import com.payflow.orchestrator.api.dto.CreatePaymentRequest;
import com.payflow.orchestrator.api.dto.PaymentResponse;
import com.payflow.orchestrator.domain.Payment;
import com.payflow.orchestrator.service.PaymentOrchestrationService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/api/v1/payments")
public class PaymentController {

    private final PaymentOrchestrationService service;

    public PaymentController(PaymentOrchestrationService service) {
        this.service = service;
    }

    /**
     * Create + authorize a payment. Clients SHOULD supply an Idempotency-Key
     * header; if omitted one is generated (so retries without a key are NOT
     * de-duplicated - the header is how a client opts into safe retries).
     */
    @PostMapping
    public ResponseEntity<PaymentResponse> create(
            @RequestHeader(value = "Idempotency-Key", required = false) String idempotencyKey,
            @Valid @RequestBody CreatePaymentRequest request) {
        String key = idempotencyKey != null ? idempotencyKey : UUID.randomUUID().toString();
        Payment payment = service.createAndAuthorize(key, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(PaymentResponse.from(payment));
    }

    @PostMapping("/{id}/capture")
    public PaymentResponse capture(@PathVariable String id,
                                   @Valid @RequestBody(required = false) CaptureRequest request) {
        var amount = request != null ? request.amount() : null;
        return PaymentResponse.from(service.capture(id, amount));
    }

    @GetMapping("/{id}")
    public PaymentResponse get(@PathVariable String id) {
        return PaymentResponse.from(service.get(id));
    }
}
