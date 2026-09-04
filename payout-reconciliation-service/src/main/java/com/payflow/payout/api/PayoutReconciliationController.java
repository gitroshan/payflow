package com.payflow.payout.api;

import com.payflow.payout.domain.Payout;
import com.payflow.payout.domain.ReconciliationRecord;
import com.payflow.payout.service.PayoutService;
import com.payflow.payout.service.ReconciliationService;
import com.payflow.payout.service.ReconciliationService.ProviderLine;
import com.payflow.payout.service.ReconciliationService.ReconciliationSummary;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.util.List;

@RestController
@RequestMapping("/api/v1")
public class PayoutReconciliationController {

    private final PayoutService payoutService;
    private final ReconciliationService reconciliationService;

    public PayoutReconciliationController(PayoutService payoutService,
                                          ReconciliationService reconciliationService) {
        this.payoutService = payoutService;
        this.reconciliationService = reconciliationService;
    }

    public record PayoutRequest(@NotBlank String merchantId, @NotNull BigDecimal amount,
                                @NotBlank String currency) {
    }

    @PostMapping("/payouts")
    public ResponseEntity<Payout> createPayout(@RequestBody PayoutRequest req) {
        Payout payout = payoutService.initiate(req.merchantId(), req.amount(), req.currency());
        return ResponseEntity.status(HttpStatus.CREATED).body(payout);
    }

    @GetMapping("/merchants/{merchantId}/payouts")
    public List<Payout> payouts(@PathVariable String merchantId) {
        return payoutService.forMerchant(merchantId);
    }

    @PostMapping("/reconciliation/run")
    public ReconciliationSummary reconcile(@RequestBody List<ProviderLine> providerLines) {
        return reconciliationService.reconcile(providerLines);
    }

    @GetMapping("/reconciliation/{batchId}")
    public List<ReconciliationRecord> batch(@PathVariable String batchId) {
        return reconciliationService.batch(batchId);
    }
}
