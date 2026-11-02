package com.divistay;

import com.divistay.domain.DateRange;
import com.divistay.inventory.RoomInventory;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class RoomInventoryTest {

    private static final Instant NOW = Instant.parse("2026-11-01T10:00:00Z");
    private static final DateRange NOV_10_12 = new DateRange(LocalDate.of(2026, 11, 10), LocalDate.of(2026, 11, 12));

    @Test
    void overlappingStayIsRejected() {
        RoomInventory inv = new RoomInventory();
        inv.hold("g1", List.of("101"), NOV_10_12, NOW.plusSeconds(1800), NOW);
        DateRange overlapping = new DateRange(LocalDate.of(2026, 11, 11), LocalDate.of(2026, 11, 13));
        assertThrows(RoomInventory.RoomsUnavailableException.class,
                () -> inv.hold("g2", List.of("101"), overlapping, null, NOW));
    }

    @Test
    void checkOutDayCanBeSomeoneElsesCheckInDay() {
        RoomInventory inv = new RoomInventory();
        inv.hold("g1", List.of("101"), NOV_10_12, null, NOW);
        DateRange next = new DateRange(LocalDate.of(2026, 11, 12), LocalDate.of(2026, 11, 14));
        inv.hold("g2", List.of("101"), next, null, NOW); // no exception
        assertFalse(inv.isAvailable("101", next, NOW));
    }

    @Test
    void holdIsAllOrNothing() {
        RoomInventory inv = new RoomInventory();
        inv.hold("g1", List.of("102"), NOV_10_12, null, NOW);
        assertThrows(RoomInventory.RoomsUnavailableException.class,
                () -> inv.hold("g2", List.of("101", "102"), NOV_10_12, null, NOW));
        assertTrue(inv.isAvailable("101", NOV_10_12, NOW), "101 must not stay held after a failed group hold");
    }

    @Test
    void temporaryHoldExpiresByItself() {
        RoomInventory inv = new RoomInventory();
        inv.hold("g1", List.of("101"), NOV_10_12, NOW.plus(Duration.ofMinutes(30)), NOW);
        assertFalse(inv.isAvailable("101", NOV_10_12, NOW.plus(Duration.ofMinutes(29))));
        assertTrue(inv.isAvailable("101", NOV_10_12, NOW.plus(Duration.ofMinutes(30))));
    }

    @Test
    void permanentHoldDoesNotExpire() {
        RoomInventory inv = new RoomInventory();
        inv.hold("g1", List.of("101"), NOV_10_12, NOW.plus(Duration.ofMinutes(30)), NOW);
        inv.makePermanent("g1");
        assertFalse(inv.isAvailable("101", NOV_10_12, NOW.plus(Duration.ofDays(1))));
    }

    @Test
    void manyGroupsRacingForOneRoomOnlyOneWins() throws Exception {
        RoomInventory inv = new RoomInventory();
        int threads = 32;
        ExecutorService pool = Executors.newFixedThreadPool(threads);
        CountDownLatch start = new CountDownLatch(1);
        AtomicInteger winners = new AtomicInteger();
        AtomicInteger losers = new AtomicInteger();
        for (int i = 0; i < threads; i++) {
            String group = "g" + i;
            pool.submit(() -> {
                start.await();
                try {
                    inv.hold(group, List.of("101"), NOV_10_12, NOW.plusSeconds(1800), NOW);
                    winners.incrementAndGet();
                } catch (RoomInventory.RoomsUnavailableException e) {
                    losers.incrementAndGet();
                }
                return null;
            });
        }
        start.countDown();
        pool.shutdown();
        assertTrue(pool.awaitTermination(10, TimeUnit.SECONDS));
        assertEquals(1, winners.get());
        assertEquals(threads - 1, losers.get());
    }
}
