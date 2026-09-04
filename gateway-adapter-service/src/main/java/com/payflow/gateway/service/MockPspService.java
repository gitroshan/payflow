package com.payflow.gateway.service;

import com.payflow.gateway.api.GatewayDtos.*;
import com.payflow.gateway.domain.GatewayTransaction;
import com.payflow.gateway.repository.GatewayTransactionRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.UUID;

/**
 * Deterministic mock of an external PSP so the platform is runnable end-to-end
 * without real credentials. Decision rules (documented for demos):
 *   - token "tok_declined"        -> declined ("card_declined")
 *   - token "tok_insufficient"    -> declined ("insufficient_funds")
 *   - amount minor units == .13   -> declined ("do_not_honor")
 *   - everything else             -> approved
 * Swap this class for a real adapter (Stripe/Adyen/etc.) without touching callers.
 */
@Service
public class MockPspService {

    private static final Logger log = LoggerFactory.getLogger(MockPspService.class);
    private final GatewayTransactionRepository repository;

    public MockPspService(GatewayTransactionRepository repository) {
        this.repository = repository;
    }

    @Transactional
    public GatewayResponse authorize(AuthorizeRequest req) {
        String decline = evaluate(req.paymentMethodToken(), req.amount());
        String reference = "psp_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        record(reference, req.paymentId(), "AUTHORIZE", req.amount(), req.currency(), decline);
        if (decline != null) {
            log.info("Authorization declined for payment {}: {}", req.paymentId(), decline);
            return new GatewayResponse(false, null, decline);
        }
        return new GatewayResponse(true, reference, null);
    }

    @Transactional
    public GatewayResponse capture(CaptureRequest req) {
        record(req.gatewayReference(), lookupPaymentId(req.gatewayReference()), "CAPTURE",
                req.amount(), "USD", null);
        return new GatewayResponse(true, req.gatewayReference(), null);
    }

    @Transactional
    public GatewayResponse refund(RefundRequest req) {
        record(req.gatewayReference() + ":rf", lookupPaymentId(req.gatewayReference()), "REFUND",
                req.amount(), "USD", null);
        return new GatewayResponse(true, req.gatewayReference(), null);
    }

    private String evaluate(String token, BigDecimal amount) {
        if ("tok_declined".equals(token)) {
            return "card_declined";
        }
        if ("tok_insufficient".equals(token)) {
            return "insufficient_funds";
        }
        if (amount.movePointRight(2).remainder(BigDecimal.valueOf(100))
                .compareTo(BigDecimal.valueOf(13)) == 0) {
            return "do_not_honor";
        }
        return null;
    }

    private String lookupPaymentId(String reference) {
        return repository.findById(reference)
                .map(GatewayTransaction::getPaymentId).orElse("unknown");
    }

    private void record(String reference, String paymentId, String operation,
                        BigDecimal amount, String currency, String decline) {
        GatewayTransaction tx = new GatewayTransaction();
        tx.setReference(reference);
        tx.setPaymentId(paymentId);
        tx.setOperation(operation);
        tx.setAmount(amount);
        tx.setCurrency(currency);
        tx.setApproved(decline == null);
        tx.setDeclineReason(decline);
        repository.save(tx);
    }
}
