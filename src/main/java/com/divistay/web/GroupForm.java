package com.divistay.web;

import com.divistay.domain.DateRange;
import com.divistay.domain.Money;
import com.divistay.domain.SplitType;
import com.divistay.service.CreateGroupCommand;
import com.divistay.split.MemberSpec;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/** Binds the "create a group" form. Member rows with an empty email are ignored. */
public class GroupForm {

    public static final int MAX_ROWS = 16;

    public static class MemberRow {
        private String name = "";
        private String email = "";
        private String roomId = "";
        private String amount = "";

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }
        public String getEmail() { return email; }
        public void setEmail(String email) { this.email = email; }
        public String getRoomId() { return roomId; }
        public void setRoomId(String roomId) { this.roomId = roomId; }
        public String getAmount() { return amount; }
        public void setAmount(String amount) { this.amount = amount; }

        boolean isBlank() {
            return (email == null || email.isBlank()) && (name == null || name.isBlank());
        }
    }

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkIn;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate checkOut;
    private String organizerName = "";
    private String organizerEmail = "";
    private List<String> roomIds = new ArrayList<>();
    private SplitType splitType = SplitType.EQUAL;
    private List<MemberRow> members = new ArrayList<>();
    /** How many people the organizer said are coming (the "who's coming" counter). */
    private int people = 2;

    /** Makes sure the form always shows {@link #MAX_ROWS} member rows. */
    public GroupForm padRows() {
        while (members.size() < MAX_ROWS) {
            members.add(new MemberRow());
        }
        return this;
    }

    public CreateGroupCommand toCommand(String hotelId) {
        List<MemberSpec> specs = new ArrayList<>();
        for (MemberRow row : members) {
            if (row.isBlank()) {
                continue;
            }
            Long amount = null;
            if (splitType == SplitType.CUSTOM) {
                amount = Money.parse(row.getAmount());
            }
            specs.add(new MemberSpec(row.getName(), row.getEmail(), row.getRoomId(), amount));
        }
        return new CreateGroupCommand(organizerName, organizerEmail, hotelId, roomIds,
                new DateRange(checkIn, checkOut), splitType, specs);
    }

    public LocalDate getCheckIn() { return checkIn; }
    public void setCheckIn(LocalDate checkIn) { this.checkIn = checkIn; }
    public LocalDate getCheckOut() { return checkOut; }
    public void setCheckOut(LocalDate checkOut) { this.checkOut = checkOut; }
    public String getOrganizerName() { return organizerName; }
    public void setOrganizerName(String organizerName) { this.organizerName = organizerName; }
    public String getOrganizerEmail() { return organizerEmail; }
    public void setOrganizerEmail(String organizerEmail) { this.organizerEmail = organizerEmail; }
    public List<String> getRoomIds() { return roomIds; }
    public void setRoomIds(List<String> roomIds) { this.roomIds = roomIds == null ? new ArrayList<>() : roomIds; }
    public SplitType getSplitType() { return splitType; }
    public void setSplitType(SplitType splitType) { this.splitType = splitType; }
    public int getPeople() { return people; }
    public void setPeople(int people) { this.people = Math.max(1, Math.min(people, MAX_ROWS)); }
    public List<MemberRow> getMembers() { return members; }
    public void setMembers(List<MemberRow> members) { this.members = members == null ? new ArrayList<>() : members; }
}
