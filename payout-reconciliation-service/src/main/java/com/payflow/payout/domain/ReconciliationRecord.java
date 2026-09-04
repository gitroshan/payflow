package com.payflow.payout.domain;

import com.payflow.common.enums.ReconciliationStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Outcome of comparing one provider settlement line against our ledger view. */
@Entity
@Table(name = "reconciliation_records")
@Getter
@Setter
@NoArgsConstructor
public class ReconciliationRecord {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 40)
    private String batchId;

    @Column(length = 36)
    private String paymentId;

    @Column(precision = 19, scale = 2)
    private BigDecimal internalAmount;

    @Column(precision = 19, scale = 2)
    private BigDecimal providerAmount;

    @Column(length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ReconciliationStatus status;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
