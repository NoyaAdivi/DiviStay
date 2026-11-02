package com.divistay.service;

import com.divistay.domain.BookingGroup;
import com.divistay.domain.DateRange;
import com.divistay.domain.DomainException;
import com.divistay.domain.GroupMembership;
import com.divistay.domain.GroupStatus;
import com.divistay.domain.Hotel;
import com.divistay.domain.Money;
import com.divistay.domain.NotFoundException;
import com.divistay.domain.Room;
import com.divistay.inventory.RoomInventory;
import com.divistay.notify.Notification;
import com.divistay.notify.NotificationService;
import com.divistay.payment.CardDetails;
import com.divistay.payment.PaymentGateway;
import com.divistay.payment.PaymentResult;
import com.divistay.repo.BookingGroupRepository;
import com.divistay.repo.HotelRepository;
import com.divistay.security.SecureTokens;
import com.divistay.split.MemberSpec;
import com.divistay.split.SplitContext;
import com.divistay.split.SplitStrategyFactory;

import java.time.Clock;
import java.time.Instant;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * The main use cases: search rooms, create a group, pay a share, remind, cancel.
 * Each change to a group runs while holding that group's lock, so a payment and the
 * expiry job can never act on the same group at the same moment.
 */
public class BookingService {

    private static final Pattern EMAIL = Pattern.compile("^[^@\\s]+@[^@\\s]+\\.[^@\\s]+$");
    private static final int MAX_MEMBERS = 30;

    private final HotelRepository hotels;
    private final BookingGroupRepository groups;
    private final RoomInventory inventory;
    private final SplitStrategyFactory splitStrategies;
    private final PaymentGateway paymentGateway;
    private final NotificationService notifications;
    private final GroupLifecycleService lifecycle;
    private final BookingSettings settings;
    private final Clock clock;

    public BookingService(HotelRepository hotels, BookingGroupRepository groups, RoomInventory inventory,
                          SplitStrategyFactory splitStrategies, PaymentGateway paymentGateway,
                          NotificationService notifications, GroupLifecycleService lifecycle,
                          BookingSettings settings, Clock clock) {
        this.hotels = hotels;
        this.groups = groups;
        this.inventory = inventory;
        this.splitStrategies = splitStrategies;
        this.paymentGateway = paymentGateway;
        this.notifications = notifications;
        this.lifecycle = lifecycle;
        this.settings = settings;
        this.clock = clock;
    }

    // ---------------------------------------------------------------- search

    public record HotelAvailability(Hotel hotel, List<Room> freeRooms) {
    }

    /** Hotels in {@code city} (all hotels if blank) with the rooms still free for the stay. */
    public List<HotelAvailability> search(String city, DateRange stay) {
        Instant now = clock.instant();
        List<HotelAvailability> result = new ArrayList<>();
        for (Hotel hotel : hotels.findByCity(city)) {
            List<Room> free = hotel.rooms().stream().filter(r -> inventory.isAvailable(r.id(), stay, now)).toList();
            if (!free.isEmpty()) {
                result.add(new HotelAvailability(hotel, free));
            }
        }
        return result;
    }

    public HotelAvailability availability(String hotelId, DateRange stay) {
        Hotel hotel = hotel(hotelId);
        Instant now = clock.instant();
        return new HotelAvailability(hotel,
                hotel.rooms().stream().filter(r -> inventory.isAvailable(r.id(), stay, now)).toList());
    }

    public Hotel hotel(String hotelId) {
        return hotels.findById(hotelId).orElseThrow(() -> new NotFoundException("Hotel not found."));
    }

    // ---------------------------------------------------------------- create

    /** Activity diagram 1: lock the rooms, split the cost, send everyone a personal payment link. */
    public CreatedGroup createGroup(CreateGroupCommand cmd) {
        validateOrganizer(cmd);
        Hotel hotel = hotel(cmd.hotelId());
        List<String> roomIds = cmd.roomIds().stream().distinct().toList();
        if (roomIds.isEmpty()) {
            throw new DomainException("Choose at least one room.");
        }
        Map<String, Long> roomCosts = new LinkedHashMap<>();
        int capacity = 0;
        for (String roomId : roomIds) {
            Room room = hotel.room(roomId);
            roomCosts.put(roomId, room.priceFor(cmd.stay()));
            capacity += room.capacity();
        }
        validateMembers(cmd.members(), capacity);
        long total = roomCosts.values().stream().mapToLong(Long::longValue).sum();

        // the strategy validates its own inputs (room assignments, custom amounts) before anything is locked
        List<Long> shares = splitStrategies.forType(cmd.splitType())
                .split(new SplitContext(total, roomCosts, cmd.members()));

        Instant now = clock.instant();
        String groupId = UUID.randomUUID().toString();
        String dashboardToken = SecureTokens.newToken();
        BookingGroup group = new BookingGroup(groupId, cmd.organizerName().trim(), cmd.organizerEmail().trim().toLowerCase(),
                SecureTokens.hash(dashboardToken), hotel.id(), roomIds, cmd.stay(), cmd.splitType(), total,
                now, now.plus(settings.paymentWindow()));

        Map<String, String> paymentTokens = new LinkedHashMap<>();
        for (int i = 0; i < cmd.members().size(); i++) {
            MemberSpec spec = cmd.members().get(i);
            String token = SecureTokens.newToken();
            String memberId = UUID.randomUUID().toString();
            group.addMember(new GroupMembership(memberId, spec.name(), spec.email(), blankToNull(spec.roomId()),
                    shares.get(i), SecureTokens.hash(token)));
            paymentTokens.put(memberId, token);
        }
        group.verifySharesMatchTotal();

        // lock last: if anything above failed, no rooms were held
        inventory.hold(groupId, roomIds, cmd.stay(), group.lockExpiresAt(), now);
        groups.save(group);

        notifications.send(new Notification(groupId, group.organizerEmail(), "Your DiviStay group is ready",
                "Rooms at " + hotel.name() + " are held for " + settings.paymentWindow().toMinutes()
                        + " minutes. Track payments on your dashboard.",
                settings.dashboardLink(dashboardToken), now));
        for (GroupMembership m : group.members()) {
            notifications.send(new Notification(groupId, m.email(),
                    group.organizerName() + " invited you to " + hotel.name(),
                    "Your share is " + Money.format(m.share()) + ". Please pay within "
                            + settings.paymentWindow().toMinutes() + " minutes to keep the rooms.",
                    settings.paymentLink(paymentTokens.get(m.id())), now));
        }
        return new CreatedGroup(group, dashboardToken, paymentTokens);
    }

    // ---------------------------------------------------------------- view

    public BookingGroup groupByDashboardToken(String token) {
        if (!SecureTokens.looksValid(token)) {
            throw new NotFoundException("This dashboard link is not valid.");
        }
        return groups.findByDashboardTokenHash(SecureTokens.hash(token))
                .orElseThrow(() -> new NotFoundException("This dashboard link is not valid."));
    }

    public record PaymentView(BookingGroup group, GroupMembership member, Hotel hotel) {
    }

    public PaymentView paymentView(String token) {
        if (!SecureTokens.looksValid(token)) {
            throw new NotFoundException("This payment link is not valid or has been replaced by a newer one.");
        }
        String hash = SecureTokens.hash(token);
        BookingGroup group = groups.findByPaymentTokenHash(hash)
                .orElseThrow(() -> new NotFoundException("This payment link is not valid or has been replaced by a newer one."));
        GroupMembership member = group.memberByTokenHash(hash).orElseThrow();
        return new PaymentView(group, member, hotel(group.hotelId()));
    }

    // ---------------------------------------------------------------- pay

    /** Activity diagram 2: a member opens their link and pays their share. */
    public PaymentResult pay(String token, CardDetails card) {
        PaymentView view = paymentView(token);
        BookingGroup group = view.group();
        GroupMembership member = view.member();
        Instant now = clock.instant();

        synchronized (group) {
            if (group.isExpired(now)) {
                throw new DomainException("Sorry, the time to pay for this booking has run out.");
            }
            if (group.status() != GroupStatus.COLLECTING_PAYMENTS) {
                throw new DomainException("This booking is " + group.status().label().toLowerCase() + "; no payment is needed.");
            }
            if (member.hasPaid()) {
                throw new DomainException("Your share is already paid. Thank you!");
            }
            card.validate(YearMonth.from(now.atZone(clock.getZone())));

            // same member + same card = same key, so a double click can't charge twice
            String idempotencyKey = member.id() + ":" + SecureTokens.hash(card.number()).substring(0, 16);
            PaymentResult result = paymentGateway.authorize(idempotencyKey, member.share(), card,
                    "DiviStay group " + group.id().substring(0, 8));
            if (!result.approved()) {
                return result;
            }
            member.markAuthorized(result.reference(), now);
            groups.save(group);

            notifications.send(new Notification(group.id(), member.email(), "Payment received",
                    "We're holding " + Money.format(member.share()) + " on your card ending " + card.last4()
                            + ". It will be charged once the hotel confirms the booking.", null, now));
            notifications.send(new Notification(group.id(), group.organizerEmail(),
                    (member.name().isBlank() ? member.email() : member.name()) + " paid their share",
                    group.paidPercent() + "% of the booking is paid.", null, now));

            if (group.isFullyPaid()) {
                group.markFullyFunded();
                inventory.makePermanent(group.id()); // the rooms no longer expire
                groups.save(group);
                notifications.send(new Notification(group.id(), group.organizerEmail(), "Everyone paid! 🎉",
                        "We're sending the reservation to the hotel now.", null, now));
                lifecycle.confirmWithHotel(group);
            }
            return result;
        }
    }

    // ---------------------------------------------------------------- organizer actions

    /** Sends a member a fresh payment link (the old one stops working). Returns the new token. */
    public String remind(String dashboardToken, String memberId) {
        BookingGroup group = groupByDashboardToken(dashboardToken);
        synchronized (group) {
            if (group.status() != GroupStatus.COLLECTING_PAYMENTS || group.isExpired(clock.instant())) {
                throw new DomainException("Reminders can only be sent while the group is collecting payments.");
            }
            GroupMembership member = group.member(memberId);
            String token = SecureTokens.newToken();
            member.replacePaymentToken(SecureTokens.hash(token));
            groups.save(group);
            notifications.send(new Notification(group.id(), member.email(), "Reminder: your share for the group trip",
                    "Please pay " + Money.format(member.share()) + " before "
                            + group.lockExpiresAt().atZone(clock.getZone()).toLocalTime().withNano(0) + ".",
                    settings.paymentLink(token), clock.instant()));
            return token;
        }
    }

    /** The organizer gives up before everyone paid: same clean-up as an expiry. */
    public void cancelByOrganizer(String dashboardToken) {
        BookingGroup group = groupByDashboardToken(dashboardToken);
        synchronized (group) {
            if (group.status() != GroupStatus.COLLECTING_PAYMENTS) {
                throw new DomainException("Only a group that is still collecting payments can be cancelled.");
            }
            lifecycle.cancel(group, "Cancelled by the organizer.");
        }
    }

    // ---------------------------------------------------------------- validation

    private static void validateOrganizer(CreateGroupCommand cmd) {
        if (cmd.organizerName() == null || cmd.organizerName().isBlank()) {
            throw new DomainException("Enter your name.");
        }
        if (cmd.organizerEmail() == null || !EMAIL.matcher(cmd.organizerEmail().trim()).matches()) {
            throw new DomainException("Enter a valid email for yourself.");
        }
        if (cmd.splitType() == null) {
            throw new DomainException("Choose how to split the cost.");
        }
    }

    private static void validateMembers(List<MemberSpec> members, int capacity) {
        if (members.isEmpty()) {
            throw new DomainException("Add at least one person to the group.");
        }
        if (members.size() > MAX_MEMBERS) {
            throw new DomainException("A group can have up to " + MAX_MEMBERS + " people.");
        }
        if (members.size() > capacity) {
            throw new DomainException("The rooms you chose sleep " + capacity + " people, but the group has "
                    + members.size() + ". Add another room.");
        }
        Set<String> emails = new HashSet<>();
        for (MemberSpec m : members) {
            if (!EMAIL.matcher(m.email()).matches()) {
                throw new DomainException("\"" + m.email() + "\" is not a valid email.");
            }
            if (!emails.add(m.email())) {
                throw new DomainException(m.email() + " appears twice in the group.");
            }
        }
    }

    private static String blankToNull(String s) {
        return s == null || s.isBlank() ? null : s;
    }
}
