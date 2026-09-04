package com.payflow.common.money;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

class MoneyTest {

    @Test
    void normalisesScaleToTwoAndUppercasesCurrency() {
        Money m = Money.of("10.5", "usd");
        assertEquals(new BigDecimal("10.50"), m.amount());
        assertEquals("USD", m.currency());
    }

    @Test
    void addsAndSubtractsSameCurrency() {
        Money a = Money.of("10.00", "USD");
        Money b = Money.of("2.50", "USD");
        assertEquals(new BigDecimal("12.50"), a.add(b).amount());
        assertEquals(new BigDecimal("7.50"), a.subtract(b).amount());
    }

    @Test
    void rejectsCurrencyMismatchOnArithmetic() {
        Money usd = Money.of("10.00", "USD");
        Money eur = Money.of("10.00", "EUR");
        assertThrows(IllegalArgumentException.class, () -> usd.add(eur));
        assertThrows(IllegalArgumentException.class, () -> usd.isGreaterThan(eur));
    }

    @Test
    void rejectsInvalidCurrencyCode() {
        assertThrows(IllegalArgumentException.class, () -> Money.of("1.00", "US"));
    }

    @Test
    void detectsNonPositiveAmounts() {
        assertTrue(Money.zero("USD").isNegativeOrZero());
        assertTrue(Money.of("-1.00", "USD").isNegativeOrZero());
        assertFalse(Money.of("0.01", "USD").isNegativeOrZero());
    }

    @Test
    void comparesAmounts() {
        assertTrue(Money.of("10.00", "USD").isGreaterThan(Money.of("9.99", "USD")));
        assertFalse(Money.of("9.99", "USD").isGreaterThan(Money.of("10.00", "USD")));
    }
}
