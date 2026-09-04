package com.payflow.refund.domain;

import com.payflow.common.enums.RefundStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "refunds")
@Getter
@Setter
@NoArgsConstructor
public class Refund {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 36)
    private String paymentId;

    @Column(nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private RefundStatus status = RefundStatus.REQUESTED;

    @Column(length = 80)
    private String reason;

    @Column(length = 80)
    private String idempotencyKey;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
