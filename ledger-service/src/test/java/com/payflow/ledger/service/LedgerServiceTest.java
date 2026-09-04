package com.payflow.ledger.service;

import com.payflow.ledger.domain.*;
import com.payflow.ledger.repository.AccountRepository;
import com.payflow.ledger.repository.JournalEntryRepository;
import com.payflow.ledger.repository.ProcessedEventRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LedgerServiceTest {

    @Mock AccountRepository accounts;
    @Mock JournalEntryRepository journals;
    @Mock ProcessedEventRepository processed;
    @InjectMocks LedgerService ledger;

    @Test
    void recordCapturePostsABalancedDoubleEntry() {
        when(processed.existsById("evt-1")).thenReturn(false);
        when(accounts.findById(anyString())).thenReturn(Optional.empty());

        ledger.recordCapture("evt-1", "pay-1", "merchant_acme", new BigDecimal("49.99"), "USD");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journals).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();

        // Two legs, and debits must equal credits.
        assertThat(entry.getPostings()).hasSize(2);
        BigDecimal debits = sum(entry, Direction.DEBIT);
        BigDecimal credits = sum(entry, Direction.CREDIT);
        assertThat(debits).isEqualByComparingTo(credits).isEqualByComparingTo("49.99");

        // Clearing account debited, merchant payable credited.
        assertThat(leg(entry, Direction.DEBIT).getAccountId()).isEqualTo("PROVIDER_CLEARING:psp:USD");
        assertThat(leg(entry, Direction.CREDIT).getAccountId()).isEqualTo("MERCHANT_PAYABLE:merchant_acme:USD");

        // Balances moved the right way (stored as debits - credits).
        ArgumentCaptor<Account> accCaptor = ArgumentCaptor.forClass(Account.class);
        verify(accounts, times(2)).save(accCaptor.capture());
        List<Account> saved = accCaptor.getAllValues();
        Account clearing = saved.stream().filter(a -> a.getType() == AccountType.PROVIDER_CLEARING).findFirst().orElseThrow();
        Account payable = saved.stream().filter(a -> a.getType() == AccountType.MERCHANT_PAYABLE).findFirst().orElseThrow();
        assertThat(clearing.getBalance()).isEqualByComparingTo("49.99");     // asset up
        assertThat(payable.getBalance()).isEqualByComparingTo("-49.99");     // liability (credit) 
        assertThat(payable.normalBalance()).isEqualByComparingTo("49.99");   // we owe the merchant

        verify(processed).save(any(ProcessedEvent.class));
    }

    @Test
    void recordCaptureIsIdempotent() {
        when(processed.existsById("evt-1")).thenReturn(true);

        ledger.recordCapture("evt-1", "pay-1", "merchant_acme", new BigDecimal("49.99"), "USD");

        verifyNoInteractions(journals);
        verify(accounts, never()).save(any());
        verify(processed, never()).save(any());
    }

    @Test
    void refundPostsTheReversingEntry() {
        when(processed.existsById("evt-r")).thenReturn(false);
        when(accounts.findById(anyString())).thenReturn(Optional.empty());

        ledger.recordRefund("evt-r", "ref-1", "pay-1", "merchant_acme", new BigDecimal("10.00"), "USD");

        ArgumentCaptor<JournalEntry> entryCaptor = ArgumentCaptor.forClass(JournalEntry.class);
        verify(journals).save(entryCaptor.capture());
        JournalEntry entry = entryCaptor.getValue();
        assertThat(sum(entry, Direction.DEBIT)).isEqualByComparingTo(sum(entry, Direction.CREDIT));
        // Refund debits the payable (we owe the merchant less) and credits clearing.
        assertThat(leg(entry, Direction.DEBIT).getAccountId()).isEqualTo("MERCHANT_PAYABLE:merchant_acme:USD");
        assertThat(leg(entry, Direction.CREDIT).getAccountId()).isEqualTo("PROVIDER_CLEARING:psp:USD");
    }

    private static BigDecimal sum(JournalEntry entry, Direction direction) {
        return entry.getPostings().stream()
                .filter(p -> p.getDirection() == direction)
                .map(Posting::getAmount)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    private static Posting leg(JournalEntry entry, Direction direction) {
        return entry.getPostings().stream()
                .filter(p -> p.getDirection() == direction)
                .findFirst().orElseThrow();
    }
}
