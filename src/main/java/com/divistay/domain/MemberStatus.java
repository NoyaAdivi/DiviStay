package com.divistay.domain;

public enum MemberStatus {
    /** Hasn't paid yet. */
    PENDING,
    /** Card authorized (money held, not taken yet). */
    AUTHORIZED,
    /** Booking confirmed and the payment captured. */
    CAPTURED,
    /** Booking cancelled; the authorization was released. */
    VOIDED
}
