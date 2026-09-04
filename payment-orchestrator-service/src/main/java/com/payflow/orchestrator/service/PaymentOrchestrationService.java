package com.payflow.orchestrator.service;

import com.payflow.common.KafkaTopics;
import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.event.PaymentAuthorizedEvent;
import com.payflow.common.event.PaymentCapturedEvent;
import com.payflow.common.event.PaymentFailedEvent;
import com.payflow.common.money.Money;
import com.payflow.orchestrator.api.dto.CreatePaymentRequest;
import com.payflow.orchestrator.domain.Payment;
import com.payflow.orchestrator.exception.PaymentException;
import com.payflow.orchestrator.gateway.*;
import com.payflow.orchestrator.messaging.OutboxWriter;
import com.payflow.orchestrator.repository.PaymentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.UUID;

/**
 * Orchestration saga for the payment lifecycle. Each externally-visible step
 * (authorize, capture) is a local transaction that (a) mutates the aggregate,
 * (b) writes a domain event to the outbox, atomically. Compensation on failure
 * marks the payment FAILED and emits a PaymentFailed event so downstream
 * services (ledger, notifications) can react.
 */
@Service
public class PaymentOrchestrationService {

    private static final Logger log = LoggerFactory.getLogger(PaymentOrchestrationService.class);

    private final PaymentRepository payments;
    private final GatewayClient gateway;
    private final OutboxWriter outbox;
    private final IdempotencyService idempotency;

    public PaymentOrchestrationService(PaymentRepository payments, GatewayClient gateway,
                                       OutboxWriter outbox, IdempotencyService idempotency) {
        this.payments = payments;
        this.gateway = gateway;
        this.outbox = outbox;
        this.idempotency = idempotency;
    }

    /**
     * Creates and authorizes a payment. Idempotent on {@code idempotencyKey}:
     * a repeated request returns the existing payment rather than charging twice.
     */
    @Transactional
    public Payment createAndAuthorize(String idempotencyKey, CreatePaymentRequest req) {
        var existing = payments.findByIdempotencyKey(idempotencyKey);
        if (existing.isPresent()) {
            log.info("Idempotent replay for key {} -> payment {}", idempotencyKey, existing.get().getId());
            return existing.get();
        }

        Payment payment = new Payment();
        payment.setId(UUID.randomUUID().toString());
        payment.setIdempotencyKey(idempotencyKey);
        payment.setMerchantId(req.merchantId());
        payment.setAmount(req.amount());
        payment.setCurrency(req.currency());
        payment.setPaymentMethodToken(req.paymentMethodToken());
        payment.setStatus(PaymentStatus.AUTHORIZING);
        payments.save(payment);

        GatewayResponse resp;
        try {
            resp = gateway.authorize(new GatewayAuthorizeRequest(
                    payment.getId(), req.merchantId(), req.amount(), req.currency(),
                    req.paymentMethodToken()));
        } catch (Exception e) {
            return fail(payment, "Gateway unavailable: " + e.getMessage());
        }

        if (!resp.approved()) {
            return fail(payment, resp.declineReason());
        }

        payment.setStatus(PaymentStatus.AUTHORIZED);
        payment.setGatewayReference(resp.reference());
        outbox.write("Payment", KafkaTopics.PAYMENT_EVENTS,
                new PaymentAuthorizedEvent(UUID.randomUUID().toString(), payment.getId(),
                        payment.getMerchantId(), payment.money(), resp.reference(), Instant.now()));

        if (req.captureImmediately()) {
            return doCapture(payment, payment.getAmount());
        }
        return payment;
    }

    @Transactional
    public Payment capture(String paymentId, BigDecimal requestedAmount) {
        Payment payment = payments.findById(paymentId).orElseThrow(() -> PaymentException.notFound(paymentId));
        if (payment.getStatus() != PaymentStatus.AUTHORIZED) {
            throw PaymentException.conflict("Payment " + paymentId + " is not in AUTHORIZED state (was "
                    + payment.getStatus() + ")");
        }
        BigDecimal amount = requestedAmount != null ? requestedAmount : payment.getAmount();
        if (amount.compareTo(payment.getAmount()) > 0) {
            throw PaymentException.conflict("Capture amount exceeds authorized amount");
        }
        return doCapture(payment, amount);
    }

    private Payment doCapture(Payment payment, BigDecimal amount) {
        payment.setStatus(PaymentStatus.CAPTURING);
        GatewayResponse resp;
        try {
            resp = gateway.capture(new GatewayCaptureRequest(payment.getGatewayReference(), amount));
        } catch (Exception e) {
            return fail(payment, "Capture failed: " + e.getMessage());
        }
        if (!resp.approved()) {
            return fail(payment, resp.declineReason());
        }
        payment.setCapturedAmount(amount);
        payment.setStatus(PaymentStatus.CAPTURED);
        outbox.write("Payment", KafkaTopics.PAYMENT_EVENTS,
                new PaymentCapturedEvent(UUID.randomUUID().toString(), payment.getId(),
                        payment.getMerchantId(), new Money(amount, payment.getCurrency()),
                        payment.getGatewayReference(), Instant.now()));
        log.info("Payment {} captured for {} {}", payment.getId(), amount, payment.getCurrency());
        return payment;
    }

    private Payment fail(Payment payment, String reason) {
        payment.setStatus(PaymentStatus.FAILED);
        payment.setFailureReason(reason);
        outbox.write("Payment", KafkaTopics.PAYMENT_EVENTS,
                new PaymentFailedEvent(UUID.randomUUID().toString(), payment.getId(),
                        payment.getMerchantId(), payment.money(), reason, Instant.now()));
        log.warn("Payment {} failed: {}", payment.getId(), reason);
        return payment;
    }

    @Transactional(readOnly = true)
    public Payment get(String id) {
        return payments.findById(id).orElseThrow(() -> PaymentException.notFound(id));
    }
}
