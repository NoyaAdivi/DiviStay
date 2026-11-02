package com.divistay.domain;

/**
 * Factory for rooms (design pattern: Factory).
 * Callers say what type of room they need; the factory owns the details of how each
 * type is built (capacity and price relative to the hotel's base rate).
 */
public class RoomFactory {

    /**
     * @param hotelBaseRate the hotel's nightly price for a standard room, in agorot
     */
    public Room create(String hotelId, String number, RoomType type, long hotelBaseRate) {
        if (hotelBaseRate <= 0) {
            throw new DomainException("A hotel's base rate must be positive.");
        }
        return switch (type) {
            case STANDARD -> new Room(id(hotelId, number), hotelId, number, type, 2, hotelBaseRate);
            case DOUBLE -> new Room(id(hotelId, number), hotelId, number, type, 2, percent(hotelBaseRate, 125));
            case FAMILY -> new Room(id(hotelId, number), hotelId, number, type, 4, percent(hotelBaseRate, 170));
            case SUITE -> new Room(id(hotelId, number), hotelId, number, type, 3, percent(hotelBaseRate, 250));
        };
    }

    private static String id(String hotelId, String number) {
        return hotelId + "-" + number;
    }

    /** Rounded to whole shekels, like hotel price lists. */
    private static long percent(long base, int percent) {
        long value = base * percent / 100;
        return Math.round(value / 100.0) * 100;
    }
}
