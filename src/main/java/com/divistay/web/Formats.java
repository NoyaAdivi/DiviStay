package com.divistay.web;

import com.divistay.domain.Money;
import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;
import org.springframework.context.i18n.LocaleContextHolder;

/** Formatting helpers for the templates, used as {@code ${@fmt.money(x)}}. */
@Component("fmt")
public class Formats {

    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm", Locale.ENGLISH);
    private final Clock clock;

    public Formats(Clock clock) {
        this.clock = clock;
    }

    public String money(long agorot) {
        return Money.format(agorot);
    }

    /** "120.50", for prefilling amount inputs */
    public String plain(long agorot) {
        return String.format(Locale.US, "%d.%02d", agorot / 100, agorot % 100);
    }

    public String date(LocalDate date) {
        // "Sat, 14 Nov 2026" in English, "יום ש׳, 14 בנוב׳ 2026" in Hebrew
        return date == null ? "" : DateTimeFormatter.ofPattern("EEE, d MMM yyyy", LocaleContextHolder.getLocale()).format(date);
    }

    public String time(Instant instant) {
        return instant == null ? "" : TIME.format(instant.atZone(clock.getZone()));
    }

    /** ISO timestamp for the countdown script. */
    public String iso(Instant instant) {
        return instant == null ? "" : instant.toString();
    }
}
