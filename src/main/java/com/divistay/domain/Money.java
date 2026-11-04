package com.divistay.domain;

import java.text.NumberFormat;
import java.util.Locale;

/**
 * Money is kept as whole agorot (1/100 of a shekel) in a {@code long}.
 * Floating point is never used for amounts, so splits always add up exactly.
 */
public final class Money {

    private Money() {
    }

    public static long shekels(long shekels) {
        return shekels * 100;
    }

    /** "₪1,234.50" */
    public static String format(long agorot) {
        NumberFormat nf = NumberFormat.getNumberInstance(Locale.US);
        nf.setMinimumFractionDigits(agorot % 100 == 0 ? 0 : 2);
        nf.setMaximumFractionDigits(2);
        return "₪" + nf.format(agorot / 100.0);
    }

    /** Parses "120", "120.5" or "120.50" into agorot. */
    public static long parse(String text) {
        if (text == null || text.isBlank()) {
            throw new DomainException("Please enter an amount.");
        }
        String t = text.trim().replace(",", "").replace("₪", "");
        if (!t.matches("\\d+(\\.\\d{1,2})?")) {
            throw new DomainException("\"" + text + "\" is not a valid amount.");
        }
        String[] parts = t.split("\\.");
        long whole = Long.parseLong(parts[0]);
        long fraction = parts.length > 1 ? Long.parseLong((parts[1] + "0").substring(0, 2)) : 0;
        return whole * 100 + fraction;
    }
}
