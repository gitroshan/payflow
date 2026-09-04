package com.payflow.refund.api;

import com.payflow.common.enums.DisputeStatus;
import com.payflow.refund.domain.Dispute;
import com.payflow.refund.domain.Refund;
import com.payflow.refund.service.DisputeService;
import com.payflow.refund.service.RefundService;
import jakarta.validation.constraints.NotBlank;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1")
public class RefundDisputeController {

    private final RefundService refundService;
    private final DisputeService disputeService;

    public RefundDisputeController(RefundService refundService, DisputeService disputeService) {
        this.refundService = refundService;
        this.disputeService = disputeService;
    }

    public record RefundRequest(@NotBlank String paymentId, BigDecimal amount, String reason) {
    }

    public record DisputeRequest(@NotBlank String paymentId, @NotBlank String reasonCode) {
    }

    public record DisputeTransitionRequest(DisputeStatus status) {
    }

    @PostMapping("/refunds")
    public ResponseEntity<Refund> createRefund(
            @RequestHeader(value = "Idempotency-Key", required = false) String key,
            @RequestBody RefundRequest req) {
        String idem = key != null ? key : UUID.randomUUID().toString();
        Refund refund = refundService.refund(idem, req.paymentId(), req.amount(), req.reason());
        return ResponseEntity.status(HttpStatus.CREATED).body(refund);
    }

    @GetMapping("/refunds")
    public List<Refund> refundsForPayment(@RequestParam String paymentId) {
        return refundService.forPayment(paymentId);
    }

    @PostMapping("/disputes")
    public ResponseEntity<Dispute> openDispute(@RequestBody DisputeRequest req) {
        Dispute dispute = disputeService.open(req.paymentId(), req.reasonCode());
        return ResponseEntity.status(HttpStatus.CREATED).body(dispute);
    }

    @PatchMapping("/disputes/{id}")
    public Dispute transition(@PathVariable String id, @RequestBody DisputeTransitionRequest req) {
        return disputeService.transition(id, req.status());
    }

    @GetMapping("/disputes")
    public List<Dispute> disputesForPayment(@RequestParam String paymentId) {
        return disputeService.forPayment(paymentId);
    }
}
