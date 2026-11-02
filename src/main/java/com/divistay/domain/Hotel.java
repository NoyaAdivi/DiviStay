package com.divistay.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * A hotel and its rooms. A room belongs to exactly one hotel (composition).
 *
 * @param name        the name shown in English (and the hotel's own name when there is no English one)
 * @param nameHe      the name shown in Hebrew
 * @param address     street and neighbourhood, when known
 * @param stars       official star rating, 1–5
 * @param rating      guest review score, 0–10
 * @param reviews     number of guest reviews
 * @param area        where in the city (a key such as "BEACH" or "OLD_TOWN"), shown translated
 * @param descVariant which generated description to use; -1 means {@link #description()} is hand-written
 * @param photoSeed   picks this hotel's photos, so every hotel looks different but always the same
 * @param photoSet    the kind of photos that fit the hotel
 * @param real        true for a real hotel (from OpenStreetMap); its rooms, prices and reviews are still simulated
 */
public record Hotel(String id, String name, String nameHe, String city, String address, String description, List<Room> rooms,
                    int stars, double rating, int reviews, String area, Set<Amenity> amenities,
                    int descVariant, int photoSeed, PhotoSet photoSet, double lat, double lon, boolean real) {

    public Hotel {
        rooms = List.copyOf(rooms);
        amenities = amenities.isEmpty() ? Set.of() : Set.copyOf(EnumSet.copyOf(amenities));
        nameHe = nameHe == null || nameHe.isBlank() ? name : nameHe;
        address = address == null ? "" : address;
        if (stars < 1 || stars > 5) {
            throw new DomainException("A hotel has 1 to 5 stars.");
        }
    }

    /** A simple hotel, as used by the tests and the hand-written demo hotels. */
    public Hotel(String id, String name, String city, String description, List<Room> rooms) {
        this(id, name, name, city, "", description, rooms, 4, 8.6, 120, "CENTER", Set.of(Amenity.WIFI), -1,
                Math.abs(id.hashCode()), PhotoSet.CITY, 0, 0, false);
    }

    public Room room(String roomId) {
        return rooms.stream()
                .filter(r -> r.id().equals(roomId))
                .findFirst()
                .orElseThrow(() -> new NotFoundException("Room " + roomId + " is not in " + name + "."));
    }

    public long fromPrice() {
        return rooms.stream().mapToLong(Room::nightlyPrice).min().orElse(0);
    }

    public boolean has(Amenity amenity) {
        return amenities.contains(amenity);
    }

    /** Amenities in a stable order, for display. */
    public List<Amenity> amenityList() {
        return amenities.isEmpty() ? List.of() : List.copyOf(EnumSet.copyOf(amenities));
    }

    /** The room types this hotel has, in the order of {@link RoomType}. */
    public List<RoomType> roomTypes() {
        return rooms.stream().map(Room::type).distinct().sorted().toList();
    }

    /** "9.2" */
    public String ratingText() {
        return String.format(Locale.ROOT, "%.1f", rating);
    }

    /** A key for the word next to the score: "superb", "great", ... */
    public String ratingWord() {
        if (rating >= 9.0) return "superb";
        if (rating >= 8.5) return "excellent";
        if (rating >= 8.0) return "veryGood";
        if (rating >= 7.0) return "good";
        return "pleasant";
    }

    /** A link to the hotel's spot on OpenStreetMap, for real hotels. */
    public String mapUrl() {
        return String.format(Locale.ROOT, "https://www.openstreetmap.org/?mlat=%.5f&mlon=%.5f#map=17/%.5f/%.5f", lat, lon, lat, lon);
    }
}
