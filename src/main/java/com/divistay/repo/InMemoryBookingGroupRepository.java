package com.divistay.repo;

import com.divistay.domain.BookingGroup;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Keeps groups in memory. The rest of the code only sees {@link BookingGroupRepository},
 * so a database-backed implementation (e.g. Spring Data JPA) can replace this without
 * changing the services.
 */
public class InMemoryBookingGroupRepository implements BookingGroupRepository {

    private final Map<String, BookingGroup> groups = new ConcurrentHashMap<>();

    @Override
    public void save(BookingGroup group) {
        groups.put(group.id(), group);
    }

    @Override
    public Optional<BookingGroup> findById(String id) {
        return Optional.ofNullable(groups.get(id));
    }

    @Override
    public Optional<BookingGroup> findByDashboardTokenHash(String hash) {
        return groups.values().stream().filter(g -> g.dashboardTokenHash().equals(hash)).findFirst();
    }

    @Override
    public Optional<BookingGroup> findByPaymentTokenHash(String hash) {
        return groups.values().stream().filter(g -> g.memberByTokenHash(hash).isPresent()).findFirst();
    }

    @Override
    public List<BookingGroup> findUnfinished() {
        return groups.values().stream().filter(g -> !g.status().isFinished()).toList();
    }

    @Override
    public List<BookingGroup> findAll() {
        return groups.values().stream().sorted(Comparator.comparing(BookingGroup::createdAt).reversed()).toList();
    }
}
