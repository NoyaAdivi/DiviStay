package com.divistay.repo;

import com.divistay.catalog.Destination;
import com.divistay.catalog.HotelCatalog;
import com.divistay.domain.Hotel;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;

/** Hotels kept in memory. The sorted list is cached, since the catalogue has thousands of hotels. */
public class InMemoryHotelRepository implements HotelRepository {

    private final Map<String, Hotel> hotels = new ConcurrentHashMap<>();
    private volatile List<Hotel> sorted;

    @Override
    public void save(Hotel hotel) {
        hotels.put(hotel.id(), hotel);
        sorted = null;
    }

    @Override
    public Optional<Hotel> findById(String id) {
        return Optional.ofNullable(hotels.get(id));
    }

    @Override
    public List<Hotel> findAll() {
        List<Hotel> s = sorted;
        if (s == null) {
            s = hotels.values().stream().sorted(Comparator.comparing(Hotel::city).thenComparing(Hotel::name)).toList();
            sorted = s;
        }
        return s;
    }

    /** Matches the city's English name, its Hebrew name ("רומא" finds Rome), or part of either. */
    @Override
    public List<Hotel> findByCity(String city) {
        if (city == null || city.isBlank()) {
            return findAll();
        }
        String c = city.trim().toLowerCase(Locale.ROOT);
        Set<String> names = HotelCatalog.matching(city).stream().map(Destination::name).collect(Collectors.toSet());
        List<Hotel> result = new ArrayList<>();
        for (Hotel h : findAll()) {
            if (names.contains(h.city()) || h.city().toLowerCase(Locale.ROOT).contains(c)) {
                result.add(h);
            }
        }
        return result;
    }

    public int count() {
        return hotels.size();
    }
}
