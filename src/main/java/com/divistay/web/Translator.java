package com.divistay.web;

import com.divistay.domain.GroupStatus;
import com.divistay.domain.Amenity;
import com.divistay.domain.Hotel;
import com.divistay.domain.MemberStatus;
import com.divistay.domain.Room;
import com.divistay.domain.RoomType;
import com.divistay.domain.SplitType;
import org.springframework.context.MessageSource;
import org.springframework.context.i18n.LocaleContextHolder;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Localizes text that comes from the core (error messages, notifications, cancellation reasons).
 * <p>
 * The core is language-neutral business logic and writes its messages in English. Rather than
 * leaking web concerns into it, the web layer recognizes each known sentence and looks up its
 * translation in {@code messages_he.properties}. Captured parts (names, amounts, nested reasons)
 * are translated recursively. Unknown text is shown as it is, so nothing can ever disappear.
 * <p>
 * Templates use it as {@code ${@tr.t(error)}}, {@code ${@tr.status(group.status())}} and so on.
 */
@Component("tr")
public class Translator {

    private record Rule(Pattern pattern, String key) {
    }

    private static final String MONEY = "(₪[\\d,]+(?:\\.\\d{2})?)";
    private static final List<Rule> RULES = new ArrayList<>();

    private static void rule(String regex, String key) {
        RULES.add(new Rule(Pattern.compile(regex), key));
    }

    private static void literal(String text, String key) {
        rule(Pattern.quote(text), key);
    }

    static {
        // --- validation and errors
        literal("Add at least one member.", "err.addMember");
        rule("Enter a positive amount for (.+)\\.", "err.positiveAmount");
        rule("The amounts add up to " + MONEY + " but the booking costs " + MONEY + " \\(" + MONEY + " missing\\)\\.", "err.amountsMissing");
        rule("The amounts add up to " + MONEY + " but the booking costs " + MONEY + " \\(" + MONEY + " too much\\)\\.", "err.amountsTooMuch");
        rule("Choose a room for (.+)\\.", "err.chooseRoomFor");
        rule("(.+) is assigned to a room that isn't in this booking\\.", "err.roomNotInBooking");
        literal("Every booked room needs at least one person staying in it.", "err.roomNeedsPerson");
        literal("Check-in can't be in the past.", "err.pastCheckIn");
        literal("Enter the name on the card.", "err.cardName");
        literal("The card number is not valid.", "err.cardNumber");
        literal("The expiry month is not valid.", "err.cardMonth");
        literal("The card has expired.", "err.cardExpired");
        literal("The CVC is not valid.", "err.cardCvc");
        literal("Choose at least one room.", "err.chooseRoom");
        literal("Sorry, a room you chose was just booked by someone else. Please choose again.", "err.roomTakenOne");
        literal("Sorry, some rooms you chose were just booked by someone else. Please choose again.", "err.roomTakenMany");
        literal("Hotel not found.", "err.hotelNotFound");
        literal("This dashboard link is not valid.", "err.dashboardLink");
        literal("This payment link is not valid or has been replaced by a newer one.", "err.paymentLink");
        literal("Sorry, the time to pay for this booking has run out.", "err.timeUp");
        rule("This booking is (.+); no payment is needed\\.", "err.noPaymentNeeded");
        literal("Your share is already paid. Thank you!", "err.alreadyPaid");
        literal("Reminders can only be sent while the group is collecting payments.", "err.remindClosed");
        literal("Only a group that is still collecting payments can be cancelled.", "err.cancelClosed");
        literal("Enter your name.", "err.yourName");
        literal("Enter a valid email for yourself.", "err.yourEmail");
        literal("Choose how to split the cost.", "err.chooseSplit");
        literal("Add at least one person to the group.", "err.addPerson");
        rule("A group can have up to (\\d+) people\\.", "err.maxPeople");
        rule("The rooms you chose sleep (\\d+) people, but the group has (\\d+)\\. Add another room\\.", "err.capacity");
        rule("\"(.+)\" is not a valid email\\.", "err.badEmail");
        rule("(.+) appears twice in the group\\.", "err.duplicateEmail");
        literal("Please choose check-in and check-out dates.", "err.chooseDates");
        literal("Check-out must be after check-in.", "err.checkoutAfter");
        literal("Please enter an amount.", "err.enterAmount");
        rule("\"(.+)\" is not a valid amount\\.", "err.badAmount");
        rule("The shares add up to " + MONEY + " but the booking costs " + MONEY + "\\.", "err.sharesMismatch");
        literal("No such member in this group.", "err.noMember");
        rule("Every member must pay a positive amount \\((.+)\\)\\.", "err.memberPositive");
        literal("This share was already paid.", "err.shareAlreadyPaid");
        rule("Room (.+) is not in (.+)\\.", "err.roomNotInHotel");
        rule("This booking is (.+)\\.", "err.bookingIs");
        literal("The card doesn't have enough credit. Try another card.", "err.noCredit");
        literal("The card was declined. Try another card.", "err.declined");
        // --- error page
        literal("Not found", "errpage.notFound");
        literal("Something isn't right", "errpage.domain");
        literal("Missing details", "errpage.missing");
        literal("Please choose your dates and try again.", "errpage.missingText");
        // --- notices on the dashboard
        rule("A reminder with a new payment link was sent to (.+)\\.", "notice.reminded");
        literal("The booking was cancelled. Rooms were released and no one was charged.", "notice.cancelled");
        literal("The hotel's system will fail the next 2 times. Pay the remaining shares to see the retry.", "notice.outage");
        // --- cancellation reasons
        literal("The payment window ended before everyone paid.", "reason.expired");
        rule("The hotel could not confirm the booking after (\\d+) attempts\\.", "reason.hotelFailed");
        literal("Cancelled by the organizer.", "reason.organizer");
        // --- notifications (demo outbox)
        literal("Booking confirmed ✅", "mail.confirmed.subject");
        rule("Your stay at (.+) is confirmed\\. Hotel confirmation number: (.+)\\. Your voucher is attached\\.", "mail.confirmed.body");
        rule("Receipt: (.+)", "mail.receipt.subject");
        rule("You paid " + MONEY + "\\. Confirmation number (.+)\\.", "mail.receipt.body");
        literal("Still waiting for the hotel", "mail.waiting.subject");
        literal("The hotel's system didn't answer. We'll keep trying automatically; your rooms stay held.", "mail.waiting.body");
        literal("Your group booking was cancelled", "mail.cancelled.subject");
        rule("(.+) The rooms were released and no one was charged\\.", "mail.cancelled.body");
        literal("Group booking cancelled", "mail.memberCancelled.subject");
        rule("(.+) The hold on your card was released\\.", "mail.memberCancelled.body");
        literal("Your DiviStay group is ready", "mail.ready.subject");
        rule("Rooms at (.+) are held for (\\d+) minutes\\. Track payments on your dashboard\\.", "mail.ready.body");
        rule("(.+) invited you to (.+)", "mail.invite.subject");
        rule("Your share is " + MONEY + "\\. Please pay within (\\d+) minutes to keep the rooms\\.", "mail.invite.body");
        literal("Payment received", "mail.paid.subject");
        rule("We're holding " + MONEY + " on your card ending (\\d{4})\\. It will be charged once the hotel confirms the booking\\.", "mail.paid.body");
        rule("(.+) paid their share", "mail.memberPaid.subject");
        rule("(\\d+)% of the booking is paid\\.", "mail.memberPaid.body");
        literal("Everyone paid! 🎉", "mail.allPaid.subject");
        literal("We're sending the reservation to the hotel now.", "mail.allPaid.body");
        literal("Reminder: your share for the group trip", "mail.reminder.subject");
        rule("Please pay " + MONEY + " before ([\\d:]+)\\.", "mail.reminder.body");
        // --- status words that appear inside sentences ("This booking is confirmed.")
        for (GroupStatus s : GroupStatus.values()) {
            rule("(?i)" + Pattern.quote(s.label()), "status." + s.name());
        }
    }

    private final MessageSource messages;

    public Translator(MessageSource messages) {
        this.messages = messages;
    }

    private static Locale locale() {
        return LocaleContextHolder.getLocale();
    }

    public boolean isHebrew() {
        return "he".equals(locale().getLanguage()) || "iw".equals(locale().getLanguage());
    }

    /** "he" or "en", for the html lang attribute. */
    public String lang() {
        return isHebrew() ? "he" : "en";
    }

    public String dir() {
        return isHebrew() ? "rtl" : "ltr";
    }

    /** Translates a sentence produced by the core; English (or unknown text) is returned unchanged. */
    public String t(String english) {
        if (english == null || english.isBlank() || !isHebrew()) {
            return english;
        }
        return translate(english, 0);
    }

    private String translate(String text, int depth) {
        if (depth > 3) {
            return text;
        }
        for (Rule rule : RULES) {
            Matcher m = rule.pattern().matcher(text);
            if (m.matches()) {
                Object[] args = new Object[m.groupCount()];
                for (int i = 0; i < args.length; i++) {
                    args[i] = translate(m.group(i + 1), depth + 1);
                }
                return messages.getMessage(rule.key(), args, text, locale());
            }
        }
        return text;
    }

    public String msg(String key, String fallback) {
        return messages.getMessage(key, null, fallback, locale());
    }

    public String status(GroupStatus status) {
        return msg("status." + status.name(), status.label());
    }

    public String memberStatus(MemberStatus status) {
        return msg("member." + status.name(), status.name());
    }

    public String split(SplitType type) {
        return msg("split." + type.name(), type.label());
    }

    public String splitDescription(SplitType type) {
        return msg("split." + type.name() + ".desc", type.description());
    }

    public String roomType(RoomType type) {
        return msg("room." + type.name(), type.label());
    }

    /** "Suite 201" / "סוויטה 201" */
    public String room(Room room) {
        return roomType(room.type()) + " " + room.number();
    }

    public String city(String city) {
        if (city == null || city.isBlank()) {
            return city;
        }
        return msg("city." + city.toLowerCase(Locale.ROOT).replace(' ', '-'), city);
    }

    public String hotelDescription(Hotel hotel) {
        if (hotel.descVariant() < 0) {
            return msg("hotel." + hotel.id() + ".desc", hotel.description());
        }
        return messages.getMessage("gen.desc." + hotel.descVariant(),
                new Object[]{area(hotel.area()), city(hotel.city())}, hotel.description(), locale());
    }

    /** The hotel's name in the visitor's language (its Hebrew name in Hebrew, when it has one). */
    public String hotelName(Hotel hotel) {
        return isHebrew() ? hotel.nameHe() : hotel.name();
    }

    /** What was searched for, translated: a city ("Rome" → "רומא"), a country ("Greece" → "יוון"), or as typed. */
    public String place(String query) {
        if (query == null || query.isBlank()) {
            return query;
        }
        var country = com.divistay.catalog.HotelCatalog.country(query);
        if (country.isPresent()) {
            return country(country.get());
        }
        var dest = com.divistay.catalog.HotelCatalog.DESTINATIONS.stream()
                .filter(d -> d.name().equalsIgnoreCase(query.trim()) || d.nameHe().equals(query.trim())).findFirst();
        return dest.map(d -> city(d.name())).orElse(query);
    }

    /** The country key of the hotel's city. */
    public String countryOf(Hotel hotel) {
        return com.divistay.catalog.HotelCatalog.destination(hotel.city()).map(d -> d.country()).orElse("");
    }

    /** "On the beach", "In the old town"... */
    public String area(String area) {
        return msg("area." + area, area);
    }

    public String amenity(Amenity amenity) {
        return msg("amenity." + amenity.name(), amenity.name());
    }

    public String country(String country) {
        return msg("country." + country, country);
    }

    /** "Superb", "Very good"... for a review score. */
    public String ratingWord(Hotel hotel) {
        return msg("rating." + hotel.ratingWord(), hotel.ratingWord());
    }

    /** "1,234 reviews" */
    public String reviews(int count) {
        return messages.getMessage("rating.reviews", new Object[]{String.format(Locale.US, "%,d", count)}, count + " reviews", locale());
    }

    /** "2 adults", "2 adults · 3 children" */
    public String guests(int adults, int children) {
        String a = adults == 1 ? msg("guests.oneAdult", "1 adult")
                : messages.getMessage("guests.adults", new Object[]{adults}, adults + " adults", locale());
        if (children == 0) {
            return a;
        }
        String c = children == 1 ? msg("guests.oneChild", "1 child")
                : messages.getMessage("guests.children", new Object[]{children}, children + " children", locale());
        return a + " · " + c;
    }

    /** "Split between 4" */
    public String payers(int payers) {
        return messages.getMessage("guests.payers", new Object[]{payers}, "Split between " + payers, locale());
    }

    public String roomTypeDescription(RoomType type) {
        return msg("room." + type.name() + ".desc", "");
    }

    public String roomTypeBeds(RoomType type) {
        return msg("room." + type.name() + ".beds", "");
    }

    public String nights(Number count) {
        long nights = count.longValue();
        return nights == 1
                ? msg("stay.oneNight", "1 night")
                : messages.getMessage("stay.nights", new Object[]{nights}, nights + " nights", locale());
    }
}
