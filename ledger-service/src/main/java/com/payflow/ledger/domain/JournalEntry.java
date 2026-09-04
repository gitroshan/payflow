package com.payflow.ledger.domain;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

/**
 * An immutable, balanced accounting transaction. The sum of debit postings must
 * equal the sum of credit postings ({@code assertBalanced}); this is the core
 * invariant of double-entry bookkeeping and is checked before persistence.
 */
@Entity
@Table(name = "journal_entries")
@Getter
@Setter
@NoArgsConstructor
public class JournalEntry {

    @Id
    @Column(length = 36)
    private String id;

    @Column(nullable = false, length = 32)
    private String referenceType;

    @Column(nullable = false, length = 40)
    private String referenceId;

    @Column(nullable = false, length = 160)
    private String description;

    @Column(nullable = false)
    private Instant createdAt = Instant.now();

    @OneToMany(mappedBy = "journalEntry", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Posting> postings = new ArrayList<>();

    public void addPosting(Posting posting) {
        posting.setJournalEntry(this);
        postings.add(posting);
    }
}
