package com.divistay.inventory;

import com.divistay.domain.DateRange;
import com.divistay.domain.DomainException;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Which rooms are taken on which nights.
 *
 * <p>A hold is either temporary (the 30-minute lock while a group collects payments) or
 * permanent (a funded or confirmed booking). Holding several rooms is all-or-nothing and
 * runs under one lock, so two groups racing for the same room can never both get it:
 * this is the overbooking protection from the activity diagram.
 */
public class RoomInventory {

    private record Hold(String groupId, String roomId, DateRange stay, Instant expiresAt) {
        boolean activeAt(Instant now) {
            return expiresAt == null || now.isBefore(expiresAt);
        }
    }

    private final List<Hold> holds = new ArrayList<>();

    /**
     * Holds all {@code roomIds} for {@code stay}, or none of them.
     *
     * @param expiresAt when the hold ends by itself; null for a permanent hold
     * @throws DomainException listing the rooms that are already taken
     */
    public synchronized void hold(String groupId, Collection<String> roomIds, DateRange stay, Instant expiresAt, Instant now) {
        if (roomIds.isEmpty()) {
            throw new DomainException("Choose at least one room.");
        }
        purgeExpired(now);
        List<String> taken = roomIds.stream()
                .filter(roomId -> isTaken(roomId, stay, now))
                .toList();
        if (!taken.isEmpty()) {
            throw new RoomsUnavailableException(taken);
        }
        for (String roomId : roomIds) {
            holds.add(new Hold(groupId, roomId, stay, expiresAt));
        }
    }

    /** Turns a group's temporary hold into a permanent one (everyone has paid). */
    public synchronized void makePermanent(String groupId) {
        holds.replaceAll(h -> h.groupId().equals(groupId) ? new Hold(h.groupId(), h.roomId(), h.stay(), null) : h);
    }

    /** Frees a group's rooms (booking cancelled). */
    public synchronized void release(String groupId) {
        holds.removeIf(h -> h.groupId().equals(groupId));
    }

    public synchronized boolean isAvailable(String roomId, DateRange stay, Instant now) {
        return !isTaken(roomId, stay, now);
    }

    public synchronized Set<String> availableOf(Collection<String> roomIds, DateRange stay, Instant now) {
        return roomIds.stream().filter(id -> !isTaken(id, stay, now)).collect(Collectors.toSet());
    }

    private boolean isTaken(String roomId, DateRange stay, Instant now) {
        return holds.stream().anyMatch(h -> h.roomId().equals(roomId) && h.activeAt(now) && h.stay().overlaps(stay));
    }

    private void purgeExpired(Instant now) {
        holds.removeIf(h -> !h.activeAt(now));
    }

    /** Thrown when some of the requested rooms are already held. */
    public static class RoomsUnavailableException extends DomainException {
        private final List<String> roomIds;

        public RoomsUnavailableException(List<String> roomIds) {
            super("Sorry, " + (roomIds.size() == 1 ? "a room you chose was" : "some rooms you chose were")
                    + " just booked by someone else. Please choose again.");
            this.roomIds = List.copyOf(roomIds);
        }

        public List<String> roomIds() {
            return roomIds;
        }
    }
}
