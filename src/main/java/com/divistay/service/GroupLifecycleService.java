package com.divistay.service;

import com.divistay.domain.BookingGroup;
import com.divistay.domain.GroupMembership;
import com.divistay.domain.GroupStatus;
import com.divistay.domain.Hotel;
import com.divistay.domain.MemberStatus;
import com.divistay.domain.Money;
import com.divistay.hotel.HotelPmsClient;
import com.divistay.inventory.RoomInventory;
import com.divistay.notify.Notification;
import com.divistay.notify.NotificationService;
import com.divistay.payment.PaymentGateway;
import com.divistay.repo.BookingGroupRepository;
import com.divistay.repo.HotelRepository;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * The background processes from activity diagram 3 (the "System/Timer" actor):
 * <ul>
 *   <li>a group whose 30 minutes ran out is cancelled: rooms released, held payments voided;</li>
 *   <li>a fully funded group is sent to the hotel; on failure it is retried with growing delays,
 *       and after too many failures it is cancelled and everyone's hold is released.</li>
 * </ul>
 * {@link #runOnce()} is called on a schedule by the web layer and directly by tests.
 */
public class GroupLifecycleService {

    private static final Logger LOG = Logger.getLogger(GroupLifecycleService.class.getName());
    private static final Duration MAX_RETRY_DELAY = Duration.ofMinutes(10);

    private final BookingGroupRepository groups;
    private final HotelRepository hotels;
    private final RoomInventory inventory;
    private final PaymentGateway paymentGateway;
    private final HotelPmsClient hotelPms;
    private final NotificationService notifications;
    private final BookingSettings settings;
    private final Clock clock;

    public GroupLifecycleService(BookingGroupRepository groups, HotelRepository hotels, RoomInventory inventory,
                                 PaymentGateway paymentGateway, HotelPmsClient hotelPms,
                                 NotificationService notifications, BookingSettings settings, Clock clock) {
        this.groups = groups;
        this.hotels = hotels;
        this.inventory = inventory;
        this.paymentGateway = paymentGateway;
        this.hotelPms = hotelPms;
        this.notifications = notifications;
        this.settings = settings;
        this.clock = clock;
    }

    /** One pass over all unfinished groups. Safe to call as often as you like. */
    public void runOnce() {
        Instant now = clock.instant();
        for (BookingGroup group : groups.findUnfinished()) {
            synchronized (group) {
                try {
                    if (group.isExpired(now)) {
                        cancel(group, "The payment window ended before everyone paid.");
                    } else if (group.status() == GroupStatus.FULLY_FUNDED
                            || (group.status() == GroupStatus.AWAITING_HOTEL_RETRY
                                && !now.isBefore(group.nextHotelAttemptAt()))) {
                        confirmWithHotel(group);
                    }
                } catch (RuntimeException e) {
                    // one broken group must not stop the job for the others
                    LOG.log(Level.SEVERE, "Lifecycle step failed for group " + group.id(), e);
                }
            }
        }
    }

    /** Sends the reservation to the hotel. Caller must hold the group's lock. */
    void confirmWithHotel(BookingGroup group) {
        Instant now = clock.instant();
        Hotel hotel = hotels.findById(group.hotelId()).orElseThrow();
        try {
            String confirmation = hotelPms.confirmReservation(new HotelPmsClient.ReservationRequest(
                    group.id(), group.hotelId(), group.roomIds(), group.stay(), group.organizerName()));
            for (GroupMembership m : group.members()) {
                if (m.status() == MemberStatus.AUTHORIZED) {
                    paymentGateway.capture(m.paymentReference());
                    m.markCaptured();
                }
            }
            group.confirm(confirmation, now);
            groups.save(group);
            notifications.send(new Notification(group.id(), group.organizerEmail(), "Booking confirmed ✅",
                    "Your stay at " + hotel.name() + " is confirmed. Hotel confirmation number: " + confirmation
                            + ". Your voucher is attached.", null, now));
            for (GroupMembership m : group.members()) {
                notifications.send(new Notification(group.id(), m.email(), "Receipt: " + hotel.name(),
                        "You paid " + Money.format(m.share()) + ". Confirmation number " + confirmation + ".", null, now));
            }
        } catch (HotelPmsClient.PmsUnavailableException e) {
            int attemptsSoFar = group.hotelAttempts() + 1;
            if (attemptsSoFar >= settings.maxHotelAttempts()) {
                group.recordHotelFailure(now); // counts the attempt before cancelling
                cancel(group, "The hotel could not confirm the booking after " + attemptsSoFar + " attempts.");
                return;
            }
            Duration delay = settings.firstRetryDelay().multipliedBy(1L << (attemptsSoFar - 1));
            if (delay.compareTo(MAX_RETRY_DELAY) > 0) {
                delay = MAX_RETRY_DELAY;
            }
            boolean firstFailure = group.status() == GroupStatus.FULLY_FUNDED;
            group.recordHotelFailure(now.plus(delay));
            groups.save(group);
            if (firstFailure) {
                notifications.send(new Notification(group.id(), group.organizerEmail(), "Still waiting for the hotel",
                        "The hotel's system didn't answer. We'll keep trying automatically; your rooms stay held.",
                        null, now));
            }
        }
    }

    /** Releases the rooms, voids every held payment and tells everyone. Caller must hold the group's lock. */
    void cancel(BookingGroup group, String reason) {
        Instant now = clock.instant();
        for (GroupMembership m : group.members()) {
            if (m.status() == MemberStatus.AUTHORIZED) {
                paymentGateway.voidAuthorization(m.paymentReference());
                m.markVoided();
            }
        }
        inventory.release(group.id());
        group.cancel(reason, now);
        groups.save(group);
        notifications.send(new Notification(group.id(), group.organizerEmail(), "Your group booking was cancelled",
                reason + " The rooms were released and no one was charged.", null, now));
        for (GroupMembership m : group.members()) {
            if (!m.email().equals(group.organizerEmail())) {
                notifications.send(new Notification(group.id(), m.email(), "Group booking cancelled",
                        reason + (m.status() == MemberStatus.VOIDED ? " The hold on your card was released." : ""),
                        null, now));
            }
        }
    }
}
