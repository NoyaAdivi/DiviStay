package com.divistay.payment;

/**
 * What DiviStay needs from any payment provider. The booking logic depends only on this
 * interface; each provider gets an adapter that translates to its own API
 * (design pattern: Adapter). Money is authorized (held) when a member pays and only
 * captured once the hotel confirms, so a cancelled booking can simply void the hold.
 */
public interface PaymentGateway {

    /**
     * Holds {@code amount} agorot on the card.
     *
     * @param idempotencyKey the same key always returns the same result, so a double click
     *                       or a retried request can never charge twice
     */
    PaymentResult authorize(String idempotencyKey, long amount, CardDetails card, String description);

    /** Takes the held money (after the hotel confirmed). */
    void capture(String reference);

    /** Releases the hold (booking cancelled). */
    void voidAuthorization(String reference);
}
