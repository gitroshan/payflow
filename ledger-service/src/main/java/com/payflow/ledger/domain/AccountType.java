package com.payflow.ledger.domain;

/**
 * Chart of accounts. Debit-normal accounts (assets) increase on debit;
 * credit-normal accounts (liabilities/revenue) increase on credit.
 */
public enum AccountType {
    PROVIDER_CLEARING(true),   // asset: funds due from the PSP
    MERCHANT_PAYABLE(false),   // liability: funds we owe the merchant
    PLATFORM_REVENUE(false),   // revenue: fees earned
    MERCHANT_RECEIVABLE(true); // asset: funds due from merchant (e.g. after refund/dispute)

    private final boolean debitNormal;

    AccountType(boolean debitNormal) {
        this.debitNormal = debitNormal;
    }

    public boolean isDebitNormal() {
        return debitNormal;
    }
}
