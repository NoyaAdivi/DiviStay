package com.divistay.payment;

import com.divistay.domain.DomainException;

import java.time.YearMonth;

/**
 * Card details typed by a member. They are passed straight to the payment gateway and
 * never stored by DiviStay; {@link #toString()} masks the number so it can't leak into logs.
 */
public record CardDetails(String holderName, String number, int expiryMonth, int expiryYear, String cvc) {

    public CardDetails {
        number = number == null ? "" : number.replaceAll("[\\s-]", "");
        holderName = holderName == null ? "" : holderName.trim();
        cvc = cvc == null ? "" : cvc.trim();
    }

    /** Basic checks before talking to the gateway: format, Luhn checksum and expiry. */
    public void validate(YearMonth currentMonth) {
        if (holderName.isBlank()) {
            throw new DomainException("Enter the name on the card.");
        }
        if (!number.matches("\\d{12,19}") || !passesLuhn(number)) {
            throw new DomainException("The card number is not valid.");
        }
        if (expiryMonth < 1 || expiryMonth > 12) {
            throw new DomainException("The expiry month is not valid.");
        }
        int year = expiryYear < 100 ? 2000 + expiryYear : expiryYear;
        if (YearMonth.of(year, expiryMonth).isBefore(currentMonth)) {
            throw new DomainException("The card has expired.");
        }
        if (!cvc.matches("\\d{3,4}")) {
            throw new DomainException("The CVC is not valid.");
        }
    }

    public String last4() {
        return number.length() >= 4 ? number.substring(number.length() - 4) : number;
    }

    static boolean passesLuhn(String digits) {
        int sum = 0;
        boolean dbl = false;
        for (int i = digits.length() - 1; i >= 0; i--) {
            int d = digits.charAt(i) - '0';
            if (dbl) {
                d *= 2;
                if (d > 9) d -= 9;
            }
            sum += d;
            dbl = !dbl;
        }
        return sum % 10 == 0;
    }

    @Override
    public String toString() {
        return "CardDetails[•••• " + last4() + "]";
    }
}
