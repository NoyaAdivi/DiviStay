package com.divistay.service;

import com.divistay.domain.BookingGroup;

import java.util.Map;

/**
 * The result of creating a group. The raw tokens exist only here, at creation time;
 * the stored group keeps just their hashes.
 *
 * @param paymentTokens payment-link token by member id
 */
public record CreatedGroup(BookingGroup group, String dashboardToken, Map<String, String> paymentTokens) {
}
