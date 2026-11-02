package com.divistay;

import com.divistay.catalog.HotelCatalog;
import com.divistay.domain.DateRange;
import com.divistay.domain.Hotel;
import com.divistay.domain.Money;
import com.divistay.domain.PhotoSet;
import com.divistay.domain.Room;
import com.divistay.domain.RoomFactory;
import com.divistay.domain.RoomType;
import com.divistay.inventory.RoomInventory;
import com.divistay.repo.InMemoryHotelRepository;
import com.divistay.service.HotelSearch;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class CatalogueAndSearchTest {

    private final Clock clock = Clock.fixed(Instant.parse("2026-11-01T10:00:00Z"), ZoneOffset.UTC);
    private final DateRange stay = new DateRange(LocalDate.of(2026, 12, 1), LocalDate.of(2026, 12, 3));

    private static final List<Hotel> HOTELS = HotelCatalog.load(new RoomFactory());

    @Test
    void theCatalogueHasOverTenThousandRealHotelsAndIsTheSameEveryRun() {
        List<Hotel> again = HotelCatalog.load(new RoomFactory());
        assertTrue(HOTELS.size() >= 10_000, "at least 10,000 hotels, got " + HOTELS.size());
        assertEquals(HOTELS.size(), (int) HOTELS.stream().map(Hotel::id).distinct().count());
        assertEquals(HOTELS.get(4321).name(), again.get(4321).name());
        assertEquals(HOTELS.get(4321).fromPrice(), again.get(4321).fromPrice());
        assertTrue(HOTELS.stream().allMatch(Hotel::real));
        assertTrue(HOTELS.stream().allMatch(h -> h.stars() >= 1 && h.stars() <= 5 && h.rating() >= 6 && h.rating() <= 10));
        assertTrue(HOTELS.stream().allMatch(h -> h.rooms().stream().anyMatch(r -> r.type() == RoomType.STANDARD)));
    }

    @Test
    void photosFitThePlace() {
        // hotels in a city that is not by the sea never get beach or tropical photos
        assertTrue(HOTELS.stream().filter(h -> h.city().equals("Jerusalem") || h.city().equals("Prague"))
                .allMatch(h -> h.photoSet() == PhotoSet.CITY));
        assertTrue(HOTELS.stream().filter(h -> h.city().equals("Santorini")).allMatch(h -> h.photoSet() == PhotoSet.GREEK));
    }

    @Test
    void searchingInHebrewOrByCountryFindsTheHotels() {
        InMemoryHotelRepository repo = new InMemoryHotelRepository();
        HOTELS.forEach(repo::save);
        List<Hotel> rome = repo.findByCity("רומא");
        assertTrue(!rome.isEmpty());
        assertTrue(rome.stream().allMatch(h -> h.city().equals("Rome")));
        assertEquals(rome.size(), repo.findByCity("rome").size());
        List<Hotel> greece = repo.findByCity("יוון");
        assertTrue(greece.stream().map(Hotel::city).distinct().count() >= 3, "all Greek cities");
        assertEquals(greece.size(), repo.findByCity("Greece").size());
        assertEquals("greece", HotelCatalog.country("Greece").orElseThrow());
    }

    @Test
    void thePlanIsTheCheapestSetOfRoomsThatSleepsEveryone() {
        RoomFactory f = new RoomFactory();
        long base = Money.shekels(500);
        List<Room> free = List.of(
                f.create("h", "101", RoomType.STANDARD, base),   // sleeps 2, 500
                f.create("h", "102", RoomType.STANDARD, base),   // sleeps 2, 500
                f.create("h", "103", RoomType.FAMILY, base),     // sleeps 4, 850
                f.create("h", "104", RoomType.SUITE, base));     // sleeps 3, 1250
        HotelSearch.Plan four = HotelSearch.plan(free, 4, stay);
        assertEquals(List.of("h-103"), four.rooms().stream().map(Room::id).toList(), "one family room beats two standard rooms");
        HotelSearch.Plan five = HotelSearch.plan(free, 5, stay);
        assertEquals(Set.of("h-101", "h-103"), Set.copyOf(five.rooms().stream().map(Room::id).toList()));
        assertEquals(2 * (Money.shekels(500) + Money.shekels(850)), five.total());
        assertNull(HotelSearch.plan(free, 12, stay), "not enough beds");
    }

    @Test
    void searchFiltersSortsAndSplitsThePriceBetweenThePayers() {
        InMemoryHotelRepository repo = new InMemoryHotelRepository();
        HOTELS.forEach(repo::save);
        HotelSearch search = new HotelSearch(repo, new RoomInventory(), clock);
        HotelSearch.Page page = search.search(new HotelSearch.Query("Athens", stay, 5, 2, Set.of(4, 5), 8.0, 0,
                Set.of(), HotelSearch.Sort.PRICE, 1));
        assertTrue(page.total() > 0);
        assertTrue(page.items().size() <= HotelSearch.PAGE_SIZE);
        long last = 0;
        for (HotelSearch.Result r : page.items()) {
            assertTrue(r.hotel().stars() >= 4 && r.hotel().rating() >= 8.0);
            assertTrue(r.plan().capacity() >= 5);
            assertEquals(r.plan().total() / 2, r.perPayer());
            assertTrue(r.plan().total() >= last, "sorted by price");
            last = r.plan().total();
        }
    }
}
