package com.divistay.hotel;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ThreadLocalRandom;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A simulated hotel system. It confirms reservations, but can be told to fail a number
 * of times first, to demonstrate (and test) the automatic retry.
 * Confirming the same group twice returns the same number (idempotent), like a real PMS
 * keyed by booking reference.
 */
public class SimulatedHotelPms implements HotelPmsClient {

    private final AtomicInteger failuresToSimulate = new AtomicInteger();
    private final Map<String, String> confirmations = new ConcurrentHashMap<>();

    /** The next {@code count} calls will fail as if the hotel were unreachable. */
    public void failNext(int count) {
        failuresToSimulate.set(count);
    }

    @Override
    public String confirmReservation(ReservationRequest request) throws PmsUnavailableException {
        if (failuresToSimulate.getAndUpdate(n -> Math.max(0, n - 1)) > 0) {
            throw new PmsUnavailableException("Hotel system did not respond (simulated).");
        }
        return confirmations.computeIfAbsent(request.groupId(),
                id -> "HTL-" + (100000 + ThreadLocalRandom.current().nextInt(900000)));
    }
}
