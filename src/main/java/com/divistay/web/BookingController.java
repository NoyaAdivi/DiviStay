package com.divistay.web;

import com.divistay.catalog.Destination;
import com.divistay.catalog.HotelCatalog;
import com.divistay.domain.Amenity;
import com.divistay.domain.DateRange;
import com.divistay.domain.Hotel;
import com.divistay.domain.Money;
import com.divistay.domain.Room;
import com.divistay.domain.RoomType;
import com.divistay.domain.DomainException;
import com.divistay.domain.SplitType;
import com.divistay.repo.HotelRepository;
import com.divistay.service.BookingService;
import com.divistay.service.CreatedGroup;
import com.divistay.service.HotelSearch;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.time.Clock;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** Search, hotel page and group creation. */
@Controller
public class BookingController {

    private final BookingService booking;
    private final HotelRepository hotels;
    private final HotelSearch search;
    private final Clock clock;

    public BookingController(BookingService booking, HotelRepository hotels, HotelSearch search, Clock clock) {
        this.booking = booking;
        this.hotels = hotels;
        this.search = search;
        this.clock = clock;
    }

    @GetMapping("/")
    public String home(Model model) {
        LocalDate today = LocalDate.now(clock);
        model.addAttribute("hotels", featured());
        model.addAttribute("popular", POPULAR.stream()
                .map(key -> HotelCatalog.byKey(key).orElseThrow())
                .map(this::cityCount).filter(c -> c.hotels() > 0).toList());
        model.addAttribute("hotelCount", hotels.findAll().size());
        model.addAttribute("guests", Guests.of(2, null, 0));
        model.addAttribute("defaultCheckIn", today.plusDays(14));
        model.addAttribute("defaultCheckOut", today.plusDays(16));
        model.addAttribute("today", today);
        return "index";
    }

    @GetMapping("/search")
    public String search(@RequestParam(defaultValue = "") String city,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                         @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                         @RequestParam(defaultValue = "2") int adults,
                         @RequestParam(required = false) List<Integer> ages,
                         @RequestParam(defaultValue = "0") int payers,
                         @RequestParam(required = false) List<Integer> stars,
                         @RequestParam(defaultValue = "0") double minRating,
                         @RequestParam(defaultValue = "0") long maxPrice,
                         @RequestParam(required = false) List<Amenity> amenities,
                         @RequestParam(defaultValue = "RECOMMENDED") HotelSearch.Sort sort,
                         @RequestParam(defaultValue = "1") int page,
                         Model model) {
        // the "Hotels" menu link has no dates: two nights, two weeks from now
        if (checkIn == null) {
            checkIn = LocalDate.now(clock).plusDays(14);
        }
        if (checkOut == null || !checkOut.isAfter(checkIn)) {
            checkOut = checkIn.plusDays(2);
        }
        DateRange stay = new DateRange(checkIn, checkOut);
        if (checkIn.isBefore(LocalDate.now(clock))) {
            throw new DomainException("Check-in can't be in the past.");
        }
        Guests guests = Guests.of(adults, ages, payers);
        HotelSearch.Query query = new HotelSearch.Query(city, stay, guests.total(), guests.payers(),
                clean(stars), minRating, Money.shekels(Math.max(0, maxPrice)), clean(amenities), sort, page);
        model.addAttribute("city", city);
        model.addAttribute("country", HotelCatalog.country(city).orElse(null));
        model.addAttribute("stay", stay);
        model.addAttribute("guests", guests);
        model.addAttribute("query", query);
        model.addAttribute("maxPrice", maxPrice);
        model.addAttribute("results", search.search(query));
        model.addAttribute("allAmenities", Amenity.values());
        model.addAttribute("sorts", HotelSearch.Sort.values());
        model.addAttribute("today", LocalDate.now(clock));
        return "search";
    }

    @GetMapping("/hotels/{hotelId}")
    public String hotel(@PathVariable String hotelId,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkIn,
                        @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate checkOut,
                        @RequestParam(defaultValue = "2") int adults,
                        @RequestParam(required = false) List<Integer> ages,
                        @RequestParam(defaultValue = "0") int payers,
                        Model model) {
        GroupForm form = new GroupForm();
        form.setCheckIn(checkIn);
        form.setCheckOut(checkOut);
        Guests guests = Guests.of(adults, ages, payers);
        form.setPeople(guests.payers());
        // start with the cheapest rooms that sleep everyone already chosen
        DateRange stay = new DateRange(checkIn, checkOut);
        HotelSearch.Plan plan = HotelSearch.plan(booking.availability(hotelId, stay).freeRooms(), guests.total(), stay);
        if (plan != null) {
            form.setRoomIds(new ArrayList<>(plan.rooms().stream().map(Room::id).toList()));
        }
        model.addAttribute("guests", guests);
        return hotelPage(hotelId, form, null, model);
    }

    @PostMapping("/hotels/{hotelId}/groups")
    public String createGroup(@PathVariable String hotelId, @ModelAttribute GroupForm form, Model model) {
        try {
            CreatedGroup created = booking.createGroup(form.toCommand(hotelId));
            return "redirect:/groups/" + created.dashboardToken() + "?created";
        } catch (DomainException e) {
            return hotelPage(hotelId, form, e.getMessage(), model);
        }
    }

    /** Empty values in a repeated parameter ("stars=") are dropped. */
    private static <T> Set<T> clean(List<T> values) {
        Set<T> out = new HashSet<>();
        if (values != null) {
            values.stream().filter(java.util.Objects::nonNull).forEach(out::add);
        }
        return out;
    }

    /** All free rooms of one type, shown as one card with a "how many" counter. */
    public record RoomGroup(RoomType type, List<Room> rooms, long nightly, long stayPrice, int sleeps, int chosen) {
    }

    /** Destinations on the home page, all with a photo of the city. */
    private static final List<String> POPULAR = List.of("tel-aviv", "jerusalem", "rome", "paris", "athens",
            "barcelona", "london", "dubai", "new-york");

    /** A destination card: the city, how many hotels it has and the cheapest room. */
    public record CityCount(Destination destination, int hotels, long fromNightly) {
    }

    /** A country on the destinations page, with its cities. */
    public record CountryGroup(String country, List<CityCount> cities, int hotels) {
    }

    private CityCount cityCount(Destination d) {
        List<Hotel> inCity = hotels.findAll().stream().filter(h -> h.city().equals(d.name())).toList();
        return new CityCount(d, inCity.size(), inCity.stream().mapToLong(Hotel::fromPrice).min().orElse(0));
    }

    /** Search by country: every country and its cities. */
    @GetMapping("/destinations")
    public String destinations(Model model) {
        LocalDate today = LocalDate.now(clock);
        List<CountryGroup> groups = new ArrayList<>();
        for (String country : HotelCatalog.COUNTRY_ORDER) {
            List<CityCount> cities = HotelCatalog.inCountry(country).stream().map(this::cityCount)
                    .filter(c -> c.hotels() > 0)
                    .sorted(Comparator.comparingInt(CityCount::hotels).reversed()).toList();
            if (!cities.isEmpty()) {
                groups.add(new CountryGroup(country, cities, cities.stream().mapToInt(CityCount::hotels).sum()));
            }
        }
        model.addAttribute("groups", groups);
        model.addAttribute("hotelCount", hotels.findAll().size());
        model.addAttribute("defaultCheckIn", today.plusDays(14));
        model.addAttribute("defaultCheckOut", today.plusDays(16));
        return "destinations";
    }

    /** The best-reviewed hotel of a few cities. */
    private List<Hotel> featured() {
        List<Hotel> list = new ArrayList<>();
        for (String c : List.of("Tel Aviv", "Jerusalem", "Eilat", "Rome", "Paris", "Athens", "Barcelona", "London")) {
            hotels.findAll().stream().filter(h -> h.city().equals(c) && h.reviews() > 300)
                    .max(Comparator.comparingDouble(Hotel::rating)).ifPresent(list::add);
        }
        return list;
    }

    private String hotelPage(String hotelId, GroupForm form, String error, Model model) {
        DateRange stay = new DateRange(form.getCheckIn(), form.getCheckOut());
        BookingService.HotelAvailability availability = booking.availability(hotelId, stay);
        model.addAttribute("availability", availability);
        Map<RoomType, List<Room>> byType = new EnumMap<>(RoomType.class);
        for (Room r : availability.freeRooms()) {
            byType.computeIfAbsent(r.type(), t -> new ArrayList<>()).add(r);
        }
        List<RoomGroup> groups = new ArrayList<>();
        byType.forEach((type, rooms) -> {
            long chosen = rooms.stream().filter(r -> form.getRoomIds().contains(r.id())).count();
            groups.add(new RoomGroup(type, rooms, rooms.get(0).nightlyPrice(), rooms.get(0).priceFor(stay),
                    rooms.get(0).capacity(), (int) chosen));
        });
        model.addAttribute("roomGroups", groups);
        model.addAttribute("stay", stay);
        model.addAttribute("form", form.padRows());
        model.addAttribute("splitTypes", SplitType.values());
        model.addAttribute("error", error);
        if (!model.containsAttribute("guests")) {
            model.addAttribute("guests", Guests.of(2, null, form.getPeople()));
        }
        return "hotel";
    }
}
