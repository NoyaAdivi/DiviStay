package com.divistay.domain;

import java.time.Instant;

/**
 * One person in a booking group and their share of the cost.
 * The same person (by email) can organize one group and be a member of another,
 * so membership is its own class instead of a User subclass (as in the class diagram).
 */
public class GroupMembership {

    private final String id;
    private final String name;
    private final String email;
    private final String roomId;          // may be null unless the split is by room
    private final long share;             // in agorot
    private String paymentTokenHash;      // SHA-256 of the personal payment link token
    private MemberStatus status = MemberStatus.PENDING;
    private String paymentReference;      // from the payment gateway
    private Instant paidAt;
    private int remindersSent;

    public GroupMembership(String id, String name, String email, String roomId, long share, String paymentTokenHash) {
        if (share <= 0) {
            throw new DomainException("Every member must pay a positive amount (" + email + ").");
        }
        this.id = id;
        this.name = name;
        this.email = email;
        this.roomId = roomId;
        this.share = share;
        this.paymentTokenHash = paymentTokenHash;
    }

    public void markAuthorized(String reference, Instant at) {
        if (status != MemberStatus.PENDING) {
            throw new DomainException("This share was already paid.");
        }
        this.status = MemberStatus.AUTHORIZED;
        this.paymentReference = reference;
        this.paidAt = at;
    }

    public void markCaptured() {
        if (status != MemberStatus.AUTHORIZED) {
            throw new IllegalStateException("Only an authorized payment can be captured.");
        }
        this.status = MemberStatus.CAPTURED;
    }

    public void markVoided() {
        if (status == MemberStatus.AUTHORIZED) {
            this.status = MemberStatus.VOIDED;
        }
    }

    /** A reminder sends a fresh link; the previous one stops working. */
    public void replacePaymentToken(String newTokenHash) {
        if (status != MemberStatus.PENDING) {
            throw new DomainException("This share was already paid.");
        }
        this.paymentTokenHash = newTokenHash;
        remindersSent++;
    }

    public boolean hasPaid() {
        return status == MemberStatus.AUTHORIZED || status == MemberStatus.CAPTURED;
    }

    public String id() { return id; }
    public String name() { return name; }
    public String email() { return email; }
    public String roomId() { return roomId; }
    public long share() { return share; }
    public String paymentTokenHash() { return paymentTokenHash; }
    public MemberStatus status() { return status; }
    public String paymentReference() { return paymentReference; }
    public Instant paidAt() { return paidAt; }
    public int remindersSent() { return remindersSent; }
}
