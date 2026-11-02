package com.divistay;

import com.divistay.domain.DomainException;
import com.divistay.domain.SplitType;
import com.divistay.split.MemberSpec;
import com.divistay.split.SplitContext;
import com.divistay.split.SplitStrategyFactory;
import org.junit.jupiter.api.Test;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SplitStrategyTest {

    private final SplitStrategyFactory factory = new SplitStrategyFactory();

    private static MemberSpec member(String email, String roomId, Long amount) {
        return new MemberSpec(email, email, roomId, amount);
    }

    private static long sum(List<Long> shares) {
        return shares.stream().mapToLong(Long::longValue).sum();
    }

    @Test
    void equalSplitNeverLosesAnAgora() {
        // ₪1,000.01 between 3 people
        SplitContext ctx = new SplitContext(100_001, Map.of("r1", 100_001L),
                List.of(member("a@x.com", null, null), member("b@x.com", null, null), member("c@x.com", null, null)));
        List<Long> shares = factory.forType(SplitType.EQUAL).split(ctx);
        assertEquals(List.of(33_334L, 33_334L, 33_333L), shares);
        assertEquals(100_001L, sum(shares));
    }

    @Test
    void roomBasedChargesEachRoomToItsGuests() {
        Map<String, Long> costs = new LinkedHashMap<>();
        costs.put("suite", 300_000L);
        costs.put("std", 100_000L);
        SplitContext ctx = new SplitContext(400_000, costs, List.of(
                member("a@x.com", "suite", null), member("b@x.com", "suite", null), member("c@x.com", "std", null)));
        List<Long> shares = factory.forType(SplitType.ROOM_BASED).split(ctx);
        assertEquals(List.of(150_000L, 150_000L, 100_000L), shares);
        assertEquals(400_000L, sum(shares));
    }

    @Test
    void roomBasedRequiresEveryRoomToHaveSomeone() {
        SplitContext ctx = new SplitContext(400_000, Map.of("suite", 300_000L, "std", 100_000L),
                List.of(member("a@x.com", "suite", null)));
        DomainException e = assertThrows(DomainException.class, () -> factory.forType(SplitType.ROOM_BASED).split(ctx));
        assertTrue(e.getMessage().contains("Every booked room"));
    }

    @Test
    void roomBasedRequiresARoomForEachMember() {
        SplitContext ctx = new SplitContext(100_000, Map.of("std", 100_000L), List.of(member("a@x.com", null, null)));
        assertThrows(DomainException.class, () -> factory.forType(SplitType.ROOM_BASED).split(ctx));
    }

    @Test
    void customSplitMustAddUpExactly() {
        SplitContext ctx = new SplitContext(100_000, Map.of("r", 100_000L),
                List.of(member("a@x.com", null, 60_000L), member("b@x.com", null, 30_000L)));
        DomainException e = assertThrows(DomainException.class, () -> factory.forType(SplitType.CUSTOM).split(ctx));
        assertTrue(e.getMessage().contains("missing"), e.getMessage());
    }

    @Test
    void customSplitAcceptsExactAmounts() {
        SplitContext ctx = new SplitContext(100_000, Map.of("r", 100_000L),
                List.of(member("a@x.com", null, 70_000L), member("b@x.com", null, 30_000L)));
        assertEquals(List.of(70_000L, 30_000L), factory.forType(SplitType.CUSTOM).split(ctx));
    }

    @Test
    void factoryHasAStrategyForEveryType() {
        for (SplitType type : SplitType.values()) {
            assertEquals(type, factory.forType(type).type());
        }
    }
}
