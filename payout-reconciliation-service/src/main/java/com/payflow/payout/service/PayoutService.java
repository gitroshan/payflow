package com.payflow.payout.service;

import com.payflow.common.KafkaTopics;
import com.payflow.common.enums.PayoutStatus;
import com.payflow.common.event.PayoutInitiatedEvent;
import com.payflow.common.money.Money;
import com.payflow.payout.domain.Payout;
import com.payflow.payout.messaging.OutboxWriter;
import com.payflow.payout.repository.Repositories.PayoutRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Initiates merchant payouts. In production this would batch a merchant's
 * available balance (from the ledger) and submit an ACH/SEPA transfer; here it
 * records the payout and emits a PayoutInitiated event for downstream systems.
 */
@Service
public class PayoutService {

    private static final Logger log = LoggerFactory.getLogger(PayoutService.class);
    private final PayoutRepository payouts;
    private final OutboxWriter outbox;

    public PayoutService(PayoutRepository payouts, OutboxWriter outbox) {
        this.payouts = payouts;
        this.outbox = outbox;
    }

    @Transactional
    public Payout initiate(String merchantId, BigDecimal amount, String currency) {
        Payout payout = new Payout();
        payout.setId(UUID.randomUUID().toString());
        payout.setMerchantId(merchantId);
        payout.setAmount(amount);
        payout.setCurrency(currency);
        payout.setStatus(PayoutStatus.PROCESSING);
        payout.setBankReference("bank_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16));
        // Simulate near-instant settlement to the merchant bank.
        payout.setStatus(PayoutStatus.PAID);
        payout.setPaidAt(Instant.now());
        payouts.save(payout);

        outbox.write(KafkaTopics.PAYOUT_EVENTS, new PayoutInitiatedEvent(
                UUID.randomUUID().toString(), payout.getId(), merchantId,
                new Money(amount, currency), Instant.now()));

        log.info("Payout {} initiated for merchant {} amount {} {}",
                payout.getId(), merchantId, amount, currency);
        return payout;
    }

    @Transactional(readOnly = true)
    public List<Payout> forMerchant(String merchantId) {
        return payouts.findByMerchantId(merchantId);
    }
}
