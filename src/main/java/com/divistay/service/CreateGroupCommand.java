package com.divistay.service;

import com.divistay.domain.DateRange;
import com.divistay.domain.SplitType;
import com.divistay.split.MemberSpec;

import java.util.List;

/** What the organizer submits on the "create group" form. The organizer should be one of the members. */
public record CreateGroupCommand(String organizerName, String organizerEmail, String hotelId, List<String> roomIds,
                                 DateRange stay, SplitType splitType, List<MemberSpec> members) {

    public CreateGroupCommand {
        roomIds = List.copyOf(roomIds);
        members = List.copyOf(members);
    }
}
