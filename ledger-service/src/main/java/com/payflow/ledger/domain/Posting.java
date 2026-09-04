package com.payflow.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

/** One leg of a journal entry: a signed movement against a single account. */
@Entity
@Table(name = "postings")
@Getter
@Setter
@NoArgsConstructor
public class Posting {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id", nullable = false)
    private JournalEntry journalEntry;

    @Column(nullable = false, length = 96)
    private String accountId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 6)
    private Direction direction;

    @Column(nullable = false, precision = 19, scale = 2)
    private BigDecimal amount;

    @Column(nullable = false, length = 3)
    private String currency;

    public static Posting of(String accountId, Direction direction, BigDecimal amount, String currency) {
        Posting p = new Posting();
        p.accountId = accountId;
        p.direction = direction;
        p.amount = amount;
        p.currency = currency;
        return p;
    }
}
