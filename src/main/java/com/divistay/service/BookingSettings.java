package com.divistay.service;

import java.time.Duration;

/**
 * Tunable business rules.
 *
 * @param paymentWindow     how long rooms stay locked while the group pays (30 minutes in the spec)
 * @param maxHotelAttempts  how many times to try the hotel before giving up and refunding
 * @param firstRetryDelay   wait before the first retry; doubles after each failure
 * @param baseUrl           used to build the links sent to members, e.g. http://localhost:8080
 */
public record BookingSettings(Duration paymentWindow, int maxHotelAttempts, Duration firstRetryDelay, String baseUrl) {

    public static BookingSettings defaults(String baseUrl) {
        return new BookingSettings(Duration.ofMinutes(30), 5, Duration.ofSeconds(30), baseUrl);
    }

    public String paymentLink(String token) {
        return baseUrl + "/pay/" + token;
    }

    public String dashboardLink(String token) {
        return baseUrl + "/groups/" + token;
    }
}
