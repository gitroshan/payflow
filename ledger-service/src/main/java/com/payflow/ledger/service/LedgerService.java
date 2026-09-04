package com.payflow.ledger.service;

import com.payflow.ledger.domain.*;
import com.payflow.ledger.repository.AccountRepository;
import com.payflow.ledger.repository.JournalEntryRepository;
import com.payflow.ledger.repository.ProcessedEventRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

/**
 * Applies balanced journal entries to the ledger. Every write is idempotent on
 * the source eventId and enforces the double-entry invariant (debits == credits)
 * before touching account balances.
 */
@Service
public class LedgerService {

    private static final Logger log = LoggerFactory.getLogger(LedgerService.class);

    private final AccountRepository accounts;
    private final JournalEntryRepository journals;
    private final ProcessedEventRepository processed;

    public LedgerService(AccountRepository accounts, JournalEntryRepository journals,
                         ProcessedEventRepository processed) {
        this.accounts = accounts;
        this.journals = journals;
        this.processed = processed;
    }

    /** Money captured: recognise a receivable from the PSP and a payable to the merchant. */
    @Transactional
    public void recordCapture(String eventId, String paymentId, String merchantId,
                              BigDecimal amount, String currency) {
        if (alreadyProcessed(eventId)) {
            return;
        }
        String clearing = Account.idOf(AccountType.PROVIDER_CLEARING, "psp", currency);
        String payable = Account.idOf(AccountType.MERCHANT_PAYABLE, merchantId, currency);
        JournalEntry entry = newEntry("PAYMENT", paymentId, "Capture of payment " + paymentId);
        entry.addPosting(Posting.of(clearing, Direction.DEBIT, amount, currency));
        entry.addPosting(Posting.of(payable, Direction.CREDIT, amount, currency));
        post(entry);
        markProcessed(eventId);
        log.info("Ledger recorded capture {} amount {} {}", paymentId, amount, currency);
    }

    /** Refund: reduce what we owe the merchant and what the PSP owes us. */
    @Transactional
    public void recordRefund(String eventId, String refundId, String paymentId, String merchantId,
                             BigDecimal amount, String currency) {
        if (alreadyProcessed(eventId)) {
            return;
        }
        String clearing = Account.idOf(AccountType.PROVIDER_CLEARING, "psp", currency);
        String payable = Account.idOf(AccountType.MERCHANT_PAYABLE, merchantId, currency);
        JournalEntry entry = newEntry("REFUND", refundId, "Refund " + refundId + " of payment " + paymentId);
        entry.addPosting(Posting.of(payable, Direction.DEBIT, amount, currency));
        entry.addPosting(Posting.of(clearing, Direction.CREDIT, amount, currency));
        post(entry);
        markProcessed(eventId);
        log.info("Ledger recorded refund {} amount {} {}", refundId, amount, currency);
    }

    // ----- internals -----

    private JournalEntry newEntry(String refType, String refId, String description) {
        JournalEntry entry = new JournalEntry();
        entry.setId(UUID.randomUUID().toString());
        entry.setReferenceType(refType);
        entry.setReferenceId(refId);
        entry.setDescription(description);
        return entry;
    }

    private void post(JournalEntry entry) {
        assertBalanced(entry);
        for (Posting posting : entry.getPostings()) {
            Account account = accounts.findById(posting.getAccountId())
                    .orElseGet(() -> createAccount(posting.getAccountId(), posting.getCurrency()));
            BigDecimal delta = posting.getDirection() == Direction.DEBIT
                    ? posting.getAmount() : posting.getAmount().negate();
            account.setBalance(account.getBalance().add(delta));
            accounts.save(account);
        }
        journals.save(entry);
    }

    private void assertBalanced(JournalEntry entry) {
        BigDecimal debits = sum(entry, Direction.DEBIT);
        BigDecimal credits = sum(entry, Direction.CREDIT);
        if (debits.compareTo(credits) != 0) {
            throw new IllegalStateException("Unbalanced journal entry " + entry.getId()
                    + ": debits=" + debits + " credits=" + credits);
        }
    }

    private BigDecimal sum(JournalEntry entry, Direction direction) {
        return entry.getPostings().stream()
                .filter(p -> p.getDirection() == direction)
                .map(Posting::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private Account createAccount(String id, String currency) {
        String[] parts = id.split(":", 3);
        Account account = new Account();
        account.setId(id);
        account.setType(AccountType.valueOf(parts[0]));
        account.setOwnerId(parts[1]);
        account.setCurrency(currency);
        return account;
    }

    private boolean alreadyProcessed(String eventId) {
        return processed.existsById(eventId);
    }

    private void markProcessed(String eventId) {
        processed.save(new ProcessedEvent(eventId));
    }

    @Transactional(readOnly = true)
    public List<Account> accountsFor(String ownerId) {
        return accounts.findByOwnerId(ownerId);
    }

    @Transactional(readOnly = true)
    public List<JournalEntry> entriesForReference(String referenceId) {
        return journals.findByReferenceId(referenceId);
    }
}
