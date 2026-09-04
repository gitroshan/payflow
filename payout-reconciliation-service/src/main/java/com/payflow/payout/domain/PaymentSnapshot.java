package com.payflow.payout.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** Captured-payment projection from payment.events, used for reconciliation. */
@Entity
@Table(name = "payment_snapshots")
@Getter
@Setter
@NoArgsConstructor
public class PaymentSnapshot {

    @Id
    @Column(length = 36)
    private String paymentId;

    @Column(nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 80)
    private String gatewayReference;
}
