package com.divistay.domain;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

/**
 * A group booking: who organizes it, which rooms are held, how the cost is split,
 * and where it is in its life cycle. All state changes go through methods that check
 * the transition is allowed, so the group can never end up in an impossible state.
 */
public class BookingGroup {

    private final String id;
    private final String organizerName;
    private final String organizerEmail;
    private final String dashboardTokenHash;
    private final String hotelId;
    private final List<String> roomIds;
    private final DateRange stay;
    private final SplitType splitType;
    private final long total;
    private final Instant createdAt;
    private final Instant lockExpiresAt;
    private final List<GroupMembership> members = new ArrayList<>();

    private GroupStatus status = GroupStatus.COLLECTING_PAYMENTS;
    private int hotelAttempts;
    private Instant nextHotelAttemptAt;
    private String hotelConfirmationNumber;
    private String cancellationReason;
    private Instant finishedAt;

    public BookingGroup(String id, String organizerName, String organizerEmail, String dashboardTokenHash,
                        String hotelId, List<String> roomIds, DateRange stay, SplitType splitType, long total,
                        Instant createdAt, Instant lockExpiresAt) {
        this.id = id;
        this.organizerName = organizerName;
        this.organizerEmail = organizerEmail;
        this.dashboardTokenHash = dashboardTokenHash;
        this.hotelId = hotelId;
        this.roomIds = List.copyOf(roomIds);
        this.stay = stay;
        this.splitType = splitType;
        this.total = total;
        this.createdAt = createdAt;
        this.lockExpiresAt = lockExpiresAt;
    }

    public void addMember(GroupMembership member) {
        members.add(member);
    }

    /** Checked once after all members are added: the shares must cover the total exactly. */
    public void verifySharesMatchTotal() {
        long sum = members.stream().mapToLong(GroupMembership::share).sum();
        if (sum != total) {
            throw new DomainException("The shares add up to " + Money.format(sum)
                    + " but the booking costs " + Money.format(total) + ".");
        }
    }

    // ----- queries -----

    public boolean isFullyPaid() {
        return !members.isEmpty() && members.stream().allMatch(GroupMembership::hasPaid);
    }

    public long paidAmount() {
        return members.stream().filter(GroupMembership::hasPaid).mapToLong(GroupMembership::share).sum();
    }

    public int paidPercent() {
        return total == 0 ? 0 : (int) (paidAmount() * 100 / total);
    }

    /** Payment window is over and the group is still waiting for money. */
    public boolean isExpired(Instant now) {
        return status == GroupStatus.COLLECTING_PAYMENTS && !now.isBefore(lockExpiresAt);
    }

    public Optional<GroupMembership> memberByTokenHash(String hash) {
        return members.stream().filter(m -> m.paymentTokenHash().equals(hash)).findFirst();
    }

    public GroupMembership member(String memberId) {
        return members.stream().filter(m -> m.id().equals(memberId)).findFirst()
                .orElseThrow(() -> new NotFoundException("No such member in this group."));
    }

    // ----- transitions -----

    public void markFullyFunded() {
        require(GroupStatus.COLLECTING_PAYMENTS);
        if (!isFullyPaid()) {
            throw new IllegalStateException("Not everyone has paid yet.");
        }
        status = GroupStatus.FULLY_FUNDED;
    }

    public void recordHotelFailure(Instant nextAttemptAt) {
        if (status != GroupStatus.FULLY_FUNDED && status != GroupStatus.AWAITING_HOTEL_RETRY) {
            throw new IllegalStateException("Hotel confirmation is not in progress.");
        }
        hotelAttempts++;
        status = GroupStatus.AWAITING_HOTEL_RETRY;
        nextHotelAttemptAt = nextAttemptAt;
    }

    public void confirm(String confirmationNumber, Instant at) {
        if (status != GroupStatus.FULLY_FUNDED && status != GroupStatus.AWAITING_HOTEL_RETRY) {
            throw new IllegalStateException("Only a fully funded group can be confirmed.");
        }
        hotelAttempts++;
        status = GroupStatus.CONFIRMED;
        hotelConfirmationNumber = confirmationNumber;
        finishedAt = at;
    }

    public void cancel(String reason, Instant at) {
        if (status.isFinished()) {
            throw new IllegalStateException("The group is already " + status.label().toLowerCase() + ".");
        }
        status = GroupStatus.CANCELLED;
        cancellationReason = reason;
        finishedAt = at;
    }

    private void require(GroupStatus expected) {
        if (status != expected) {
            throw new DomainException("This booking is " + status.label().toLowerCase() + ".");
        }
    }

    // ----- getters -----

    public String id() { return id; }
    public String organizerName() { return organizerName; }
    public String organizerEmail() { return organizerEmail; }
    public String dashboardTokenHash() { return dashboardTokenHash; }
    public String hotelId() { return hotelId; }
    public List<String> roomIds() { return roomIds; }
    public DateRange stay() { return stay; }
    public SplitType splitType() { return splitType; }
    public long total() { return total; }
    public Instant createdAt() { return createdAt; }
    public Instant lockExpiresAt() { return lockExpiresAt; }
    public List<GroupMembership> members() { return Collections.unmodifiableList(members); }
    public GroupStatus status() { return status; }
    public int hotelAttempts() { return hotelAttempts; }
    public Instant nextHotelAttemptAt() { return nextHotelAttemptAt; }
    public String hotelConfirmationNumber() { return hotelConfirmationNumber; }
    public String cancellationReason() { return cancellationReason; }
    public Instant finishedAt() { return finishedAt; }
}
