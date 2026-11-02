package com.divistay.domain;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;

/**
 * A stay from check-in (inclusive) to check-out (exclusive).
 * Two stays overlap only if they share at least one night, so a check-out and a
 * check-in on the same day do not conflict.
 */
public record DateRange(LocalDate checkIn, LocalDate checkOut) {

    public DateRange {
        if (checkIn == null || checkOut == null) {
            throw new DomainException("Please choose check-in and check-out dates.");
        }
        if (!checkOut.isAfter(checkIn)) {
            throw new DomainException("Check-out must be after check-in.");
        }
    }

    public long nights() {
        return ChronoUnit.DAYS.between(checkIn, checkOut);
    }

    public boolean overlaps(DateRange other) {
        return checkIn.isBefore(other.checkOut) && other.checkIn.isBefore(checkOut);
    }
}
