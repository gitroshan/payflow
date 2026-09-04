package com.payflow.orchestrator.domain;

import com.payflow.common.enums.PaymentStatus;
import com.payflow.common.money.Money;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/**
 * Aggregate root for a payment. The orchestrator drives this entity through the
 * saga: CREATED -> AUTHORIZED -> CAPTURED -> SETTLED, with FAILED/VOIDED branches.
 * Optimistic locking ({@code @Version}) guards against concurrent state transitions.
 */
@Entity
@Table(name = "payments",
        indexes = @Index(name = "ux_payments_idempotency", columnList = "idempotencyKey", unique = true))
@Getter
@Setter
@NoArgsConstructor
public class Payment {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 80)
    private String idempotencyKey;

    @Column(nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal capturedAmount = BigDecimal.ZERO;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal refundedAmount = BigDecimal.ZERO;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private PaymentStatus status = PaymentStatus.CREATED;

    @Column(length = 80)
    private String gatewayReference;

    @Column(length = 255)
    private String failureReason;

    @Column(length = 64)
    private String paymentMethodToken;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    @Version
    private long version;

    @PreUpdate
    void onUpdate() {
        this.updatedAt = Instant.now();
    }

    public Money money() {
        return new Money(amount, currency);
    }
}
