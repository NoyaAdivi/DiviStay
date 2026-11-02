package com.divistay.domain;

public enum RoomType {
    STANDARD("Standard room"),
    DOUBLE("Double room"),
    FAMILY("Family room"),
    SUITE("Suite");

    private final String label;

    RoomType(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }
}
