package com.divistay.split;

/**
 * A person the organizer adds to the group.
 *
 * @param roomId       which booked room they stay in (needed for the by-room split)
 * @param customAmount their amount in agorot (needed for the custom split), or null
 */
public record MemberSpec(String name, String email, String roomId, Long customAmount) {

    public MemberSpec {
        name = name == null ? "" : name.trim();
        email = email == null ? "" : email.trim().toLowerCase();
    }
}
