package com.payflow.refund.domain;

import com.payflow.common.enums.DisputeStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "disputes")
@Getter
@Setter
@NoArgsConstructor
public class Dispute {

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

    @Column(nullable = false, length = 32)
    private String reasonCode;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DisputeStatus status = DisputeStatus.OPEN;

    @Column(nullable = false)
    private Instant openedAt = Instant.now();

    @Column
    private Instant resolvedAt;
}
