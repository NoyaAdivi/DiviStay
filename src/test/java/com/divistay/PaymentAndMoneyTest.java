package com.divistay;

import com.divistay.domain.DomainException;
import com.divistay.domain.Money;
import com.divistay.payment.CardDetails;
import com.divistay.payment.MockPayClient;
import com.divistay.payment.MockPayGatewayAdapter;
import com.divistay.payment.PaymentResult;
import org.junit.jupiter.api.Test;

import java.time.YearMonth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentAndMoneyTest {

    private static final CardDetails CARD = new CardDetails("N A", "4242424242424242", 1, 2031, "999");

    @Test
    void adapterTranslatesProviderCodes() {
        MockPayGatewayAdapter gateway = new MockPayGatewayAdapter(new MockPayClient());
        assertTrue(gateway.authorize("k1", 1000, CARD, "test").approved());
        PaymentResult declined = gateway.authorize("k2", 1000,
                new CardDetails("N A", "4000000000000002", 1, 2031, "999"), "test");
        assertFalse(declined.approved());
        PaymentResult noFunds = gateway.authorize("k3", 1000,
                new CardDetails("N A", "4000000000009995", 1, 2031, "999"), "test");
        assertTrue(noFunds.declineReason().contains("enough credit"));
    }

    @Test
    void sameIdempotencyKeyNeverChargesTwice() {
        MockPayClient client = new MockPayClient();
        MockPayGatewayAdapter gateway = new MockPayGatewayAdapter(client);
        PaymentResult first = gateway.authorize("same", 5000, CARD, "test");
        PaymentResult second = gateway.authorize("same", 5000, CARD, "test");
        assertEquals(first.reference(), second.reference());
    }

    @Test
    void cannotCaptureAVoidedPayment() {
        MockPayGatewayAdapter gateway = new MockPayGatewayAdapter(new MockPayClient());
        String ref = gateway.authorize("k", 5000, CARD, "test").reference();
        gateway.voidAuthorization(ref);
        assertThrows(IllegalStateException.class, () -> gateway.capture(ref));
    }

    @Test
    void cardValidation() {
        YearMonth now = YearMonth.of(2026, 11);
        CARD.validate(now); // valid
        assertThrows(DomainException.class, () -> new CardDetails("N A", "4242424242424241", 1, 2031, "999").validate(now));
        assertThrows(DomainException.class, () -> new CardDetails("N A", "4242424242424242", 10, 2026, "999").validate(now));
        assertThrows(DomainException.class, () -> new CardDetails("", "4242424242424242", 1, 2031, "999").validate(now));
        assertTrue(CARD.toString().contains("4242") && !CARD.toString().contains("42424242"));
    }

    @Test
    void moneyParsingAndFormatting() {
        assertEquals(12_050, Money.parse("120.5"));
        assertEquals(12_005, Money.parse("120.05"));
        assertEquals(100_000, Money.parse("1,000"));
        assertEquals("₪1,234.50", Money.format(123_450));
        assertThrows(DomainException.class, () -> Money.parse("12.345"));
        assertThrows(DomainException.class, () -> Money.parse("-5"));
    }
}
