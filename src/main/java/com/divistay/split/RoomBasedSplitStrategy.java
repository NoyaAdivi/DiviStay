package com.divistay.split;

import com.divistay.domain.DomainException;
import com.divistay.domain.SplitType;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Each room's cost is shared by the people staying in it. Someone in a suite pays more
 * than someone in a standard room, which is fairer than an equal split.
 */
public class RoomBasedSplitStrategy implements SplitStrategy {

    @Override
    public SplitType type() {
        return SplitType.ROOM_BASED;
    }

    @Override
    public List<Long> split(SplitContext context) {
        List<MemberSpec> members = context.members();
        if (members.isEmpty()) {
            throw new DomainException("Add at least one member.");
        }

        // group member positions by room
        Map<String, List<Integer>> byRoom = new LinkedHashMap<>();
        for (int i = 0; i < members.size(); i++) {
            String roomId = members.get(i).roomId();
            if (roomId == null || roomId.isBlank()) {
                throw new DomainException("Choose a room for " + display(members.get(i)) + ".");
            }
            if (!context.roomCosts().containsKey(roomId)) {
                throw new DomainException(display(members.get(i)) + " is assigned to a room that isn't in this booking.");
            }
            byRoom.computeIfAbsent(roomId, k -> new ArrayList<>()).add(i);
        }
        for (String roomId : context.roomCosts().keySet()) {
            if (!byRoom.containsKey(roomId)) {
                throw new DomainException("Every booked room needs at least one person staying in it.");
            }
        }

        Long[] shares = new Long[members.size()];
        byRoom.forEach((roomId, positions) -> {
            List<Long> parts = SplitStrategy.divideEvenly(context.roomCosts().get(roomId), positions.size());
            for (int k = 0; k < positions.size(); k++) {
                shares[positions.get(k)] = parts.get(k);
            }
        });
        return List.of(shares);
    }

    private static String display(MemberSpec m) {
        return m.name().isBlank() ? m.email() : m.name();
    }
}
