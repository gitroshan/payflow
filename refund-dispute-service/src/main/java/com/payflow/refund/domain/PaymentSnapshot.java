package com.payflow.refund.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * Local read model of a captured payment, materialised from payment.events.
 * The refund service never queries the orchestrator directly - it keeps its own
 * projection, which is the essence of a loosely-coupled distributed design.
 */
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
    private BigDecimal capturedAmount;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, length = 80)
    private String gatewayReference;

    public BigDecimal refundableAmount() {
        return capturedAmount.subtract(refundedAmount);
    }
}
