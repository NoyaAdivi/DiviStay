package com.divistay.repo;

import com.divistay.domain.BookingGroup;

import java.util.List;
import java.util.Optional;

public interface BookingGroupRepository {

    void save(BookingGroup group);

    Optional<BookingGroup> findById(String id);

    Optional<BookingGroup> findByDashboardTokenHash(String hash);

    Optional<BookingGroup> findByPaymentTokenHash(String hash);

    /** Groups that still need the background job (not confirmed or cancelled). */
    List<BookingGroup> findUnfinished();

    List<BookingGroup> findAll();
}
