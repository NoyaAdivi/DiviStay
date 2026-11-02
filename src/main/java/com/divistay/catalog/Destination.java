package com.divistay.catalog;

/**
 * A city people can search for.
 *
 * @param key      used in message keys and photo choice, e.g. "tel-aviv"
 * @param name     English name, also what is stored on each hotel
 * @param nameHe   Hebrew name, so a search in Hebrew finds it too
 * @param country  key of the country ("israel", "greece", ...)
 * @param baseRate typical nightly price of a 3-star standard room, in shekels
 * @param seaside  whether hotels can be on the beach
 * @param weight   how many hotels this city gets, relative to the others
 */
public record Destination(String key, String name, String nameHe, String country, int baseRate,
                          boolean seaside, int weight) {

    public boolean matches(String query) {
        String q = query.trim().toLowerCase(java.util.Locale.ROOT);
        return name.toLowerCase(java.util.Locale.ROOT).contains(q) || nameHe.contains(query.trim());
    }
}
