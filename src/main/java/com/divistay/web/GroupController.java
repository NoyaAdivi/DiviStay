package com.divistay.web;

import com.divistay.domain.BookingGroup;
import com.divistay.domain.DomainException;
import com.divistay.domain.GroupMembership;
import com.divistay.domain.Hotel;
import com.divistay.domain.Room;
import com.divistay.hotel.SimulatedHotelPms;
import com.divistay.notify.NotificationService;
import com.divistay.service.BookingService;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.HashMap;
import java.util.Map;

/** The organizer's private dashboard (the link itself is the key). */
@Controller
public class GroupController {

    private final BookingService booking;
    private final NotificationService notifications;
    private final SimulatedHotelPms hotelPms;

    public GroupController(BookingService booking, NotificationService notifications, SimulatedHotelPms hotelPms) {
        this.booking = booking;
        this.notifications = notifications;
        this.hotelPms = hotelPms;
    }

    @GetMapping("/groups/{token}")
    public String dashboard(@PathVariable String token, Model model) {
        BookingGroup group = booking.groupByDashboardToken(token);
        Hotel hotel = booking.hotel(group.hotelId());
        Map<String, Room> roomsById = new HashMap<>();
        for (Room r : hotel.rooms()) {
            roomsById.put(r.id(), r);
        }
        model.addAttribute("token", token);
        model.addAttribute("group", group);
        model.addAttribute("hotel", hotel);
        model.addAttribute("roomsById", roomsById);
        model.addAttribute("outbox", notifications.sentFor(group.id()));
        return "dashboard";
    }

    @PostMapping("/groups/{token}/members/{memberId}/remind")
    public String remind(@PathVariable String token, @PathVariable String memberId, RedirectAttributes redirect) {
        try {
            booking.remind(token, memberId);
            GroupMembership m = booking.groupByDashboardToken(token).member(memberId);
            redirect.addFlashAttribute("notice", "A reminder with a new payment link was sent to " + m.email() + ".");
        } catch (DomainException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/groups/" + token;
    }

    @PostMapping("/groups/{token}/cancel")
    public String cancel(@PathVariable String token, RedirectAttributes redirect) {
        try {
            booking.cancelByOrganizer(token);
            redirect.addFlashAttribute("notice", "The booking was cancelled. Rooms were released and no one was charged.");
        } catch (DomainException e) {
            redirect.addFlashAttribute("error", e.getMessage());
        }
        return "redirect:/groups/" + token;
    }

    /** Demo tool: make the hotel's system fail twice, to watch the automatic retry. */
    @PostMapping("/groups/{token}/demo/hotel-outage")
    public String simulateOutage(@PathVariable String token, RedirectAttributes redirect) {
        booking.groupByDashboardToken(token); // only from a valid dashboard
        hotelPms.failNext(2);
        redirect.addFlashAttribute("notice", "The hotel's system will fail the next 2 times. Pay the remaining shares to see the retry.");
        return "redirect:/groups/" + token;
    }
}
