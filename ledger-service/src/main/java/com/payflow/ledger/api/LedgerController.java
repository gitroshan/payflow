package com.payflow.ledger.api;

import com.payflow.ledger.domain.Account;
import com.payflow.ledger.domain.JournalEntry;
import com.payflow.ledger.domain.Posting;
import com.payflow.ledger.service.LedgerService;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@RestController
@RequestMapping("/api/v1/ledger")
public class LedgerController {

    private final LedgerService ledger;

    public LedgerController(LedgerService ledger) {
        this.ledger = ledger;
    }

    public record AccountView(String id, String type, String ownerId, String currency,
                              BigDecimal rawBalance, BigDecimal normalBalance) {
    }

    public record PostingView(String accountId, String direction, BigDecimal amount, String currency) {
    }

    public record JournalView(String id, String referenceType, String referenceId,
                              String description, Instant createdAt, List<PostingView> postings) {
    }

    @GetMapping("/accounts/{ownerId}")
    public List<AccountView> accounts(@PathVariable String ownerId) {
        return ledger.accountsFor(ownerId).stream().map(a -> new AccountView(
                a.getId(), a.getType().name(), a.getOwnerId(), a.getCurrency(),
                a.getBalance(), a.normalBalance())).toList();
    }

    @GetMapping("/entries/{referenceId}")
    public List<JournalView> entries(@PathVariable String referenceId) {
        return ledger.entriesForReference(referenceId).stream().map(j -> new JournalView(
                j.getId(), j.getReferenceType(), j.getReferenceId(), j.getDescription(),
                j.getCreatedAt(),
                j.getPostings().stream().map(this::toPostingView).toList())).toList();
    }

    private PostingView toPostingView(Posting p) {
        return new PostingView(p.getAccountId(), p.getDirection().name(), p.getAmount(), p.getCurrency());
    }
}
