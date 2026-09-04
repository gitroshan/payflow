package com.payflow.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/**
 * A ledger account. Its {@code balance} is stored as (sum of debits - sum of
 * credits); callers interpret the sign via {@link AccountType#isDebitNormal()}.
 * Identity is the natural key type:owner:currency to keep lookups deterministic.
 */
@Entity
@Table(name = "accounts")
@Getter
@Setter
@NoArgsConstructor
public class Account {

    @Id
    @Column(length = 96)
    private String id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 32)
    private AccountType type;

    @Column(nullable = false, length = 64)
    private String ownerId;

    @Column(nullable = false, length = 3)
    private String currency;

    @Column(nullable = false, precision = 21, scale = 2)
    private BigDecimal balance = BigDecimal.ZERO;

    @Version
    private long version;

    public static String idOf(AccountType type, String ownerId, String currency) {
        return type + ":" + ownerId + ":" + currency;
    }

    /** Balance expressed in the account's natural (normal) sign. */
    public BigDecimal normalBalance() {
        return type.isDebitNormal() ? balance : balance.negate();
    }
}
