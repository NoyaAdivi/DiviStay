package com.divistay.domain;

/** What a hotel offers. The emoji is shown next to the translated name. */
public enum Amenity {
    WIFI("📶"), POOL("🏊"), BREAKFAST("🥐"), PARKING("🅿️"), SPA("💆"), GYM("🏋️"),
    KIDS_CLUB("🧸"), BEACH("🏖️"), KOSHER("✡️"), ACCESSIBLE("♿"), PETS("🐾"), RESTAURANT("🍽️");

    private final String emoji;

    Amenity(String emoji) {
        this.emoji = emoji;
    }

    public String emoji() {
        return emoji;
    }
}
