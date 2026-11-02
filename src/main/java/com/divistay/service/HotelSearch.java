package com.divistay.service;

import com.divistay.domain.Amenity;
import com.divistay.domain.DateRange;
import com.divistay.domain.Hotel;
import com.divistay.domain.Room;
import com.divistay.inventory.RoomInventory;
import com.divistay.repo.HotelRepository;

import java.time.Clock;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Set;

/**
 * Search like a real booking site: filters, sorting, pages, and for every hotel the cheapest
 * set of free rooms that sleeps the whole group, with what each payer would pay.
 */
public class HotelSearch {

    public static final int PAGE_SIZE = 20;

    public enum Sort { RECOMMENDED, PRICE, RATING, STARS }

    /**
     * @param guests       everyone who needs a bed (adults and children)
     * @param payers       how many people split the bill
     * @param stars        allowed star ratings; empty = any
     * @param maxNightly   most a room may cost per night, in agorot; 0 = no limit
     * @param page         1-based
     */
    public record Query(String city, DateRange stay, int guests, int payers, Set<Integer> stars, double minRating,
                        long maxNightly, Set<Amenity> amenities, Sort sort, int page) {
        public Query {
            guests = Math.max(1, Math.min(guests, 40));
            payers = Math.max(1, Math.min(payers, 30));
            stars = stars == null ? Set.of() : Set.copyOf(stars);
            amenities = amenities == null ? Set.of() : Set.copyOf(amenities);
            sort = sort == null ? Sort.RECOMMENDED : sort;
            page = Math.max(1, page);
        }
    }

    /** The cheapest free rooms that sleep everyone. */
    public record Plan(List<Room> rooms, long total, int capacity) {
        public String roomIdsCsv() {
            return String.join(",", rooms.stream().map(Room::id).toList());
        }
    }

    public record Result(Hotel hotel, List<Room> freeRooms, Plan plan, long perPayer) {
        public long fromNightly() {
            return freeRooms.stream().mapToLong(Room::nightlyPrice).min().orElse(0);
        }
    }

    public record Page(List<Result> items, int total, int page, int pages) {
        public boolean hasPrev() { return page > 1; }
        public boolean hasNext() { return page < pages; }
        public int from() { return total == 0 ? 0 : (page - 1) * PAGE_SIZE + 1; }
        public int to() { return Math.min(total, page * PAGE_SIZE); }
    }

    private final HotelRepository hotels;
    private final RoomInventory inventory;
    private final Clock clock;

    public HotelSearch(HotelRepository hotels, RoomInventory inventory, Clock clock) {
        this.hotels = hotels;
        this.inventory = inventory;
        this.clock = clock;
    }

    public Page search(Query q) {
        Instant now = clock.instant();
        List<Result> matches = new ArrayList<>();
        for (Hotel h : hotels.findByCity(q.city())) {
            if (!q.stars().isEmpty() && !q.stars().contains(h.stars())) continue;
            if (h.rating() < q.minRating()) continue;
            if (!h.amenities().containsAll(q.amenities())) continue;
            List<Room> free = new ArrayList<>();
            for (Room r : h.rooms()) {
                if ((q.maxNightly() <= 0 || r.nightlyPrice() <= q.maxNightly()) && inventory.isAvailable(r.id(), q.stay(), now)) {
                    free.add(r);
                }
            }
            Plan plan = plan(free, q.guests(), q.stay());
            if (plan == null) continue;
            matches.add(new Result(h, free, plan, plan.total() / q.payers()));
        }
        matches.sort(comparator(q.sort()));
        int pages = Math.max(1, (matches.size() + PAGE_SIZE - 1) / PAGE_SIZE);
        int page = Math.min(q.page(), pages);
        List<Result> items = matches.subList((page - 1) * PAGE_SIZE, Math.min(matches.size(), page * PAGE_SIZE));
        return new Page(List.copyOf(items), matches.size(), page, pages);
    }

    private static Comparator<Result> comparator(Sort sort) {
        Comparator<Result> byPrice = Comparator.comparingLong(r -> r.plan().total());
        return switch (sort) {
            case PRICE -> byPrice.thenComparing(r -> -r.hotel().rating());
            case RATING -> Comparator.<Result>comparingDouble(r -> -r.hotel().rating()).thenComparing(byPrice);
            case STARS -> Comparator.<Result>comparingInt(r -> -r.hotel().stars()).thenComparing(byPrice);
            // good reviews with many of them, a little bonus for value; the hand-written demo hotels first
            case RECOMMENDED -> Comparator.<Result>comparingInt(r -> r.hotel().descVariant() < 0 ? 0 : 1)
                    .thenComparingDouble(r -> -(r.hotel().rating() + Math.log10(r.hotel().reviews() + 1) * 0.35
                            - r.plan().total() / (double) Math.max(1, r.plan().rooms().size()) / 400_000.0))
                    .thenComparing(r -> r.hotel().id());
        };
    }

    /**
     * The cheapest set of rooms (each used at most once) whose beds add up to at least {@code guests}.
     * A small knapsack: capacity is capped at {@code guests}, so it stays fast for thousands of hotels.
     *
     * @return null when the free rooms can't sleep everyone
     */
    public static Plan plan(List<Room> free, int guests, DateRange stay) {
        if (free.isEmpty()) return null;
        int cap = guests;
        long[] cost = new long[cap + 1];
        List<List<Room>> chosen = new ArrayList<>();
        for (int c = 0; c <= cap; c++) {
            cost[c] = c == 0 ? 0 : Long.MAX_VALUE;
            chosen.add(List.of());
        }
        for (Room room : free) {
            long price = room.priceFor(stay);
            for (int c = cap; c >= 0; c--) {
                if (cost[c] == Long.MAX_VALUE) continue;
                int to = Math.min(cap, c + room.capacity());
                long candidate = cost[c] + price;
                if (candidate < cost[to] || (candidate == cost[to] && chosen.get(c).size() + 1 < chosen.get(to).size())) {
                    cost[to] = candidate;
                    List<Room> next = new ArrayList<>(chosen.get(c));
                    next.add(room);
                    chosen.set(to, next);
                }
            }
        }
        if (cost[cap] == Long.MAX_VALUE) return null;
        List<Room> rooms = chosen.get(cap);
        return new Plan(List.copyOf(rooms), cost[cap], rooms.stream().mapToInt(Room::capacity).sum());
    }
}
