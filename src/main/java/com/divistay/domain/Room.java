package com.divistay.domain;

/**
 * A room in a hotel. Rooms are created only through {@link RoomFactory}, which decides
 * capacity and price by type, so the rules for each room type live in one place.
 */
public record Room(String id, String hotelId, String number, RoomType type, int capacity, long nightlyPrice) {

    public long priceFor(DateRange stay) {
        return nightlyPrice * stay.nights();
    }

    public String label() {
        return type.label() + " " + number;
    }
}
