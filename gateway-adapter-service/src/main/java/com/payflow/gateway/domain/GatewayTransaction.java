package com.payflow.gateway.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

/** Audit record of every operation the adapter performed against a PSP. */
@Entity
@Table(name = "gateway_transactions")
@Getter
@Setter
@NoArgsConstructor
public class GatewayTransaction {

    @Id
    @Column(length = 40)
    private String reference;

    @Column(nullable = false, length = 36)
    private String paymentId;

    @Column(nullable = false, length = 16)
    private String operation;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false)
    private boolean approved;

    @Column(length = 128)
    private String declineReason;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();
}
