package com.divistay.split;

import com.divistay.domain.SplitType;

import java.util.List;

/**
 * How the booking total is divided between members (design pattern: Strategy).
 * {@code BookingService} only knows this interface, so a new way of splitting can be
 * added without touching the booking logic.
 */
public interface SplitStrategy {

    SplitType type();

    /**
     * @return each member's share in agorot, in the same order as {@code context.members()};
     *         the shares always add up to exactly {@code context.total()}
     */
    List<Long> split(SplitContext context);

    /**
     * Divides {@code amount} into {@code parts} whole-agora shares that add up exactly.
     * The leftover agorot go one each to the first members, so no agora is lost.
     */
    static List<Long> divideEvenly(long amount, int parts) {
        long base = amount / parts;
        long remainder = amount % parts;
        Long[] shares = new Long[parts];
        for (int i = 0; i < parts; i++) {
            shares[i] = base + (i < remainder ? 1 : 0);
        }
        return List.of(shares);
    }
}
