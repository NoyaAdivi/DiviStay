package com.divistay.domain;

/** The life of a group booking, as in the activity diagrams. */
public enum GroupStatus {
    /** Rooms are locked; waiting for members to pay before the timer runs out. */
    COLLECTING_PAYMENTS("Collecting payments", false),
    /** Everyone paid; sending the reservation to the hotel. */
    FULLY_FUNDED("Fully funded", false),
    /** The hotel did not answer or refused; will try again automatically. */
    AWAITING_HOTEL_RETRY("Waiting for the hotel (retrying)", false),
    /** The hotel confirmed; payments captured and vouchers sent. */
    CONFIRMED("Confirmed", true),
    /** Time ran out or the hotel never confirmed; rooms released, payments voided. */
    CANCELLED("Cancelled", true);

    private final String label;
    private final boolean finished;

    GroupStatus(String label, boolean finished) {
        this.label = label;
        this.finished = finished;
    }

    public String label() {
        return label;
    }

    public boolean isFinished() {
        return finished;
    }
}
