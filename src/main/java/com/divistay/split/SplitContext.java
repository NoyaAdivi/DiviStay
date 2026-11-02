package com.divistay.split;

import java.util.List;
import java.util.Map;

/**
 * Everything a split strategy needs to know.
 *
 * @param total     the booking total in agorot
 * @param roomCosts the full-stay cost of each booked room, by room id
 * @param members   the people paying, in the order the organizer listed them
 */
public record SplitContext(long total, Map<String, Long> roomCosts, List<MemberSpec> members) {

    public SplitContext {
        roomCosts = Map.copyOf(roomCosts);
        members = List.copyOf(members);
    }
}
