package com.payflow.payout.domain;

import com.payflow.common.enums.PayoutStatus;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "payouts")
@Getter
@Setter
@NoArgsConstructor
public class Payout {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 64)
    private String merchantId;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private PayoutStatus status = PayoutStatus.SCHEDULED;

    @Column(length = 64)
    private String bankReference;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @Column
    private Instant paidAt;
}
