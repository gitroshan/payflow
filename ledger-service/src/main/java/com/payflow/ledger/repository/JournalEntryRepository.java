package com.payflow.ledger.repository;

import com.payflow.ledger.domain.JournalEntry;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface JournalEntryRepository extends JpaRepository<JournalEntry, String> {
    List<JournalEntry> findByReferenceId(String referenceId);
}
