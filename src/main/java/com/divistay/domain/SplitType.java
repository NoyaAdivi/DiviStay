package com.divistay.domain;

public enum SplitType {
    EQUAL("Equal split", "Everyone pays the same share of the total."),
    ROOM_BASED("By room", "Each room's cost is shared by the people staying in it."),
    CUSTOM("Custom amounts", "The organizer sets each person's amount; it must add up to the total.");

    private final String label;
    private final String description;

    SplitType(String label, String description) {
        this.label = label;
        this.description = description;
    }

    public String label() {
        return label;
    }

    public String description() {
        return description;
    }
}
