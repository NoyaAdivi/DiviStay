package com.divistay;

import com.divistay.domain.BookingGroup;
import com.divistay.domain.DateRange;
import com.divistay.domain.DomainException;
import com.divistay.domain.GroupStatus;
import com.divistay.domain.Hotel;
import com.divistay.domain.MemberStatus;
import com.divistay.domain.Money;
import com.divistay.domain.NotFoundException;
import com.divistay.domain.RoomFactory;
import com.divistay.domain.RoomType;
import com.divistay.domain.SplitType;
import com.divistay.hotel.SimulatedHotelPms;
import com.divistay.inventory.RoomInventory;
import com.divistay.notify.OutboxNotificationService;
import com.divistay.payment.CardDetails;
import com.divistay.payment.MockPayClient;
import com.divistay.payment.MockPayGatewayAdapter;
import com.divistay.payment.PaymentResult;
import com.divistay.repo.InMemoryBookingGroupRepository;
import com.divistay.repo.InMemoryHotelRepository;
import com.divistay.service.BookingService;
import com.divistay.service.BookingSettings;
import com.divistay.service.CreateGroupCommand;
import com.divistay.service.CreatedGroup;
import com.divistay.service.GroupLifecycleService;
import com.divistay.split.MemberSpec;
import com.divistay.split.SplitStrategyFactory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** End-to-end scenarios from the activity diagrams, without a web server. */
class BookingFlowTest {

    private static final CardDetails GOOD_CARD = new CardDetails("Noya Adivi", "4242 4242 4242 4242", 12, 2030, "123");
    private static final CardDetails DECLINED_CARD = new CardDetails("Noya Adivi", "4000 0000 0000 0002", 12, 2030, "123");
    private static final DateRange STAY = new DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3)); // 2 nights

    private TestClock clock;
    private InMemoryBookingGroupRepository groups;
    private RoomInventory inventory;
    private MockPayClient mockPay;
    private SimulatedHotelPms hotelPms;
    private OutboxNotificationService outbox;
    private GroupLifecycleService lifecycle;
    private BookingService booking;
    private Hotel hotel;

    @BeforeEach
    void setUp() {
        clock = new TestClock(Instant.parse("2026-11-01T10:00:00Z"));
        InMemoryHotelRepository hotels = new InMemoryHotelRepository();
        RoomFactory rooms = new RoomFactory();
        long base = Money.shekels(500);
        hotel = new Hotel("tlv", "Sea Breeze", "Tel Aviv", "Test hotel", List.of(
                rooms.create("tlv", "101", RoomType.STANDARD, base),
                rooms.create("tlv", "201", RoomType.SUITE, base)));
        hotels.save(hotel);
        groups = new InMemoryBookingGroupRepository();
        inventory = new RoomInventory();
        mockPay = new MockPayClient();
        hotelPms = new SimulatedHotelPms();
        outbox = new OutboxNotificationService();
        BookingSettings settings = new BookingSettings(Duration.ofMinutes(30), 3, Duration.ofSeconds(30), "http://test");
        lifecycle = new GroupLifecycleService(groups, hotels, inventory, new MockPayGatewayAdapter(mockPay),
                hotelPms, outbox, settings, clock);
        booking = new BookingService(hotels, groups, inventory, new SplitStrategyFactory(),
                new MockPayGatewayAdapter(mockPay), outbox, lifecycle, settings, clock);
    }

    private CreatedGroup createEqualGroupOfThree() {
        return booking.createGroup(new CreateGroupCommand("Noya", "noya@example.com", "tlv", List.of("tlv-101", "tlv-201"),
                STAY, SplitType.EQUAL, List.of(
                        new MemberSpec("Noya", "noya@example.com", null, null),
                        new MemberSpec("Amit", "amit@example.com", null, null),
                        new MemberSpec("Amir", "amir@example.com", null, null))));
    }

    private void payAll(CreatedGroup created) {
        for (String token : created.paymentTokens().values()) {
            assertTrue(booking.pay(token, GOOD_CARD).approved());
        }
    }

    @Test
    void creatingAGroupLocksRoomsAndSendsEveryoneALink() {
        CreatedGroup created = createEqualGroupOfThree();
        BookingGroup g = created.group();
        // standard ₪500 + suite ₪1,250, two nights = ₪3,500
        assertEquals(Money.shekels(3500), g.total());
        assertEquals(g.total(), g.members().stream().mapToLong(m -> m.share()).sum());
        assertEquals(GroupStatus.COLLECTING_PAYMENTS, g.status());
        assertFalse(inventory.isAvailable("tlv-101", STAY, clock.instant()));
        assertEquals(4, outbox.sentFor(g.id()).size()); // organizer + 3 members
        assertTrue(outbox.sentFor(g.id()).stream().anyMatch(n -> n.link() != null && n.link().startsWith("http://test/pay/")));
    }

    @Test
    void whenEveryonePaysTheBookingIsConfirmedAndCaptured() {
        CreatedGroup created = createEqualGroupOfThree();
        payAll(created);
        BookingGroup g = created.group();
        assertEquals(GroupStatus.CONFIRMED, g.status());
        assertNotNull(g.hotelConfirmationNumber());
        g.members().forEach(m -> {
            assertEquals(MemberStatus.CAPTURED, m.status());
            assertEquals("captured", mockPay.stateOf(m.paymentReference()));
        });
    }

    @Test
    void afterThirtyMinutesUnpaidGroupIsCancelledAndHoldsAreVoided() {
        CreatedGroup created = createEqualGroupOfThree();
        String firstToken = created.paymentTokens().values().iterator().next();
        booking.pay(firstToken, GOOD_CARD);

        clock.advance(Duration.ofMinutes(30));
        lifecycle.runOnce();

        BookingGroup g = created.group();
        assertEquals(GroupStatus.CANCELLED, g.status());
        assertTrue(inventory.isAvailable("tlv-101", STAY, clock.instant()), "rooms must be released");
        long voided = g.members().stream().filter(m -> m.status() == MemberStatus.VOIDED).count();
        assertEquals(1, voided);
        g.members().stream().filter(m -> m.paymentReference() != null)
                .forEach(m -> assertEquals("voided", mockPay.stateOf(m.paymentReference())));
    }

    @Test
    void payingAfterTheWindowClosedIsRefused() {
        CreatedGroup created = createEqualGroupOfThree();
        clock.advance(Duration.ofMinutes(31));
        String token = created.paymentTokens().values().iterator().next();
        DomainException e = assertThrows(DomainException.class, () -> booking.pay(token, GOOD_CARD));
        assertTrue(e.getMessage().contains("run out"));
    }

    @Test
    void secondGroupCannotTakeTheSameRooms() {
        createEqualGroupOfThree();
        assertThrows(RoomInventory.RoomsUnavailableException.class, () -> booking.createGroup(new CreateGroupCommand(
                "Dana", "dana@example.com", "tlv", List.of("tlv-101"), STAY, SplitType.EQUAL,
                List.of(new MemberSpec("Dana", "dana@example.com", null, null)))));
    }

    @Test
    void declinedCardLeavesTheShareUnpaidAndCanBeRetried() {
        CreatedGroup created = createEqualGroupOfThree();
        String token = created.paymentTokens().values().iterator().next();
        PaymentResult declined = booking.pay(token, DECLINED_CARD);
        assertFalse(declined.approved());
        assertTrue(booking.pay(token, GOOD_CARD).approved());
    }

    @Test
    void aShareCannotBePaidTwice() {
        CreatedGroup created = createEqualGroupOfThree();
        String token = created.paymentTokens().values().iterator().next();
        booking.pay(token, GOOD_CARD);
        assertThrows(DomainException.class, () -> booking.pay(token, GOOD_CARD));
    }

    @Test
    void hotelFailuresAreRetriedThenConfirmed() {
        hotelPms.failNext(2);
        CreatedGroup created = createEqualGroupOfThree();
        payAll(created); // first attempt fails
        BookingGroup g = created.group();
        assertEquals(GroupStatus.AWAITING_HOTEL_RETRY, g.status());

        clock.advance(Duration.ofSeconds(30));
        lifecycle.runOnce(); // second attempt fails
        assertEquals(GroupStatus.AWAITING_HOTEL_RETRY, g.status());

        clock.advance(Duration.ofSeconds(59));
        lifecycle.runOnce(); // backoff is 60s now: too early, nothing happens
        assertEquals(2, g.hotelAttempts());

        clock.advance(Duration.ofSeconds(1));
        lifecycle.runOnce(); // third attempt succeeds
        assertEquals(GroupStatus.CONFIRMED, g.status());
    }

    @Test
    void hotelThatNeverConfirmsLeadsToCancellationWithoutCharges() {
        hotelPms.failNext(100);
        CreatedGroup created = createEqualGroupOfThree();
        payAll(created);
        for (int i = 0; i < 5; i++) {
            clock.advance(Duration.ofMinutes(10));
            lifecycle.runOnce();
        }
        BookingGroup g = created.group();
        assertEquals(GroupStatus.CANCELLED, g.status());
        assertEquals(3, g.hotelAttempts());
        g.members().forEach(m -> assertEquals(MemberStatus.VOIDED, m.status()));
        assertTrue(inventory.isAvailable("tlv-201", STAY, clock.instant()));
    }

    @Test
    void reminderReplacesThePaymentLink() {
        CreatedGroup created = createEqualGroupOfThree();
        String memberId = created.paymentTokens().keySet().iterator().next();
        String oldToken = created.paymentTokens().get(memberId);
        String newToken = booking.remind(created.dashboardToken(), memberId);
        assertThrows(NotFoundException.class, () -> booking.paymentView(oldToken));
        assertTrue(booking.pay(newToken, GOOD_CARD).approved());
    }

    @Test
    void organizerCanCancelWhileCollecting() {
        CreatedGroup created = createEqualGroupOfThree();
        booking.cancelByOrganizer(created.dashboardToken());
        assertEquals(GroupStatus.CANCELLED, created.group().status());
        assertTrue(inventory.isAvailable("tlv-101", STAY, clock.instant()));
    }

    @Test
    void groupLargerThanRoomCapacityIsRejected() {
        DomainException e = assertThrows(DomainException.class, () -> booking.createGroup(new CreateGroupCommand(
                "Noya", "noya@example.com", "tlv", List.of("tlv-101"), STAY, SplitType.EQUAL, List.of(
                new MemberSpec("A", "a@example.com", null, null),
                new MemberSpec("B", "b@example.com", null, null),
                new MemberSpec("C", "c@example.com", null, null)))));
        assertTrue(e.getMessage().contains("sleep 2"), e.getMessage());
        assertTrue(inventory.isAvailable("tlv-101", STAY, clock.instant()), "nothing may be locked after a failed create");
    }

    @Test
    void invalidLinksAreRejected() {
        assertThrows(NotFoundException.class, () -> booking.paymentView("not-a-token"));
        assertThrows(NotFoundException.class, () -> booking.groupByDashboardToken("x".repeat(43)));
    }
}
