package com.divistay.web;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Who is travelling, from the search box: adults, the ages of the children, and how many
 * people split the payment (by default, every adult pays a share).
 */
public record Guests(int adults, List<Integer> childAges, int payers) {

    public static final int MAX = 16;

    public static Guests of(int adults, List<Integer> ages, int payers) {
        int a = Math.max(1, Math.min(adults, MAX));
        List<Integer> kids = ages == null ? List.of()
                : ages.stream().filter(x -> x != null && x >= 0 && x <= 17).limit(MAX).toList();
        int p = payers <= 0 ? a : Math.max(1, Math.min(payers, a + kids.size()));
        return new Guests(a, kids, p);
    }

    public int children() {
        return childAges.size();
    }

    /** Everyone who needs a bed. Babies under 2 sleep in a cot, so they don't need one. */
    public int total() {
        return adults + (int) childAges.stream().filter(age -> age >= 2).count();
    }

    /** "4,9" for links and the search box script. */
    public String agesCsv() {
        return childAges.stream().map(String::valueOf).collect(Collectors.joining(","));
    }
}
