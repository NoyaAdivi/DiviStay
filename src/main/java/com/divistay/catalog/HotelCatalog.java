package com.divistay.catalog;

import com.divistay.domain.Amenity;
import com.divistay.domain.Hotel;
import com.divistay.domain.Money;
import com.divistay.domain.PhotoSet;
import com.divistay.domain.Room;
import com.divistay.domain.RoomFactory;
import com.divistay.domain.RoomType;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * The hotels on the site: real hotels from OpenStreetMap (names, stars when known, and where they are),
 * read from {@code catalog/hotels.tsv}. Rooms, prices, reviews and facilities are simulated, always the
 * same for the same hotel, because a demo has no access to real availability.
 * Hotel data © OpenStreetMap contributors, under the Open Database License (ODbL).
 */
public final class HotelCatalog {

    public static final List<Destination> DESTINATIONS = List.of(
            // Israel
            new Destination("tel-aviv", "Tel Aviv", "תל אביב", "israel", 620, true, 70),
            new Destination("jerusalem", "Jerusalem", "ירושלים", "israel", 540, false, 60),
            new Destination("eilat", "Eilat", "אילת", "israel", 560, true, 55),
            new Destination("haifa", "Haifa", "חיפה", "israel", 420, true, 35),
            new Destination("dead-sea", "Dead Sea", "ים המלח", "israel", 640, true, 25),
            new Destination("tiberias", "Tiberias", "טבריה", "israel", 430, true, 30),
            new Destination("netanya", "Netanya", "נתניה", "israel", 460, true, 25),
            new Destination("herzliya", "Herzliya", "הרצליה", "israel", 650, true, 18),
            new Destination("nazareth", "Nazareth", "נצרת", "israel", 350, false, 14),
            new Destination("akko", "Akko", "עכו", "israel", 390, true, 12),
            new Destination("safed", "Safed", "צפת", "israel", 380, false, 14),
            new Destination("caesarea", "Caesarea", "קיסריה", "israel", 600, true, 8),
            new Destination("mitzpe-ramon", "Mitzpe Ramon", "מצפה רמון", "israel", 480, false, 9),
            new Destination("ashkelon", "Ashkelon", "אשקלון", "israel", 380, true, 12),
            new Destination("ashdod", "Ashdod", "אשדוד", "israel", 390, true, 12),
            new Destination("beersheba", "Beersheba", "באר שבע", "israel", 330, false, 12),
            new Destination("rosh-pina", "Rosh Pina", "ראש פינה", "israel", 470, false, 10),
            new Destination("zichron-yaakov", "Zichron Yaakov", "זכרון יעקב", "israel", 480, false, 8),
            new Destination("bat-yam", "Bat Yam", "בת ים", "israel", 410, true, 10),
            new Destination("nahariya", "Nahariya", "נהריה", "israel", 360, true, 10),
            new Destination("ramat-gan", "Ramat Gan", "רמת גן", "israel", 480, false, 10),
            new Destination("arad", "Arad", "ערד", "israel", 300, false, 6),
            // Abroad
            new Destination("athens", "Athens", "אתונה", "greece", 430, true, 55),
            new Destination("rhodes", "Rhodes", "רודוס", "greece", 470, true, 40),
            new Destination("crete", "Crete", "כרתים", "greece", 460, true, 40),
            new Destination("santorini", "Santorini", "סנטוריני", "greece", 720, true, 25),
            new Destination("thessaloniki", "Thessaloniki", "סלוניקי", "greece", 360, true, 25),
            new Destination("larnaca", "Larnaca", "לרנקה", "cyprus", 390, true, 30),
            new Destination("paphos", "Paphos", "פאפוס", "cyprus", 420, true, 25),
            new Destination("limassol", "Limassol", "לימסול", "cyprus", 480, true, 25),
            new Destination("rome", "Rome", "רומא", "italy", 560, false, 60),
            new Destination("milan", "Milan", "מילאנו", "italy", 540, false, 40),
            new Destination("venice", "Venice", "ונציה", "italy", 640, false, 30),
            new Destination("florence", "Florence", "פירנצה", "italy", 560, false, 30),
            new Destination("paris", "Paris", "פריז", "france", 680, false, 60),
            new Destination("nice", "Nice", "ניס", "france", 600, true, 30),
            new Destination("barcelona", "Barcelona", "ברצלונה", "spain", 560, true, 55),
            new Destination("madrid", "Madrid", "מדריד", "spain", 490, false, 40),
            new Destination("lisbon", "Lisbon", "ליסבון", "portugal", 470, true, 40),
            new Destination("london", "London", "לונדון", "uk", 760, false, 60),
            new Destination("amsterdam", "Amsterdam", "אמסטרדם", "netherlands", 690, false, 45),
            new Destination("berlin", "Berlin", "ברלין", "germany", 470, false, 50),
            new Destination("munich", "Munich", "מינכן", "germany", 520, false, 30),
            new Destination("prague", "Prague", "פראג", "czechia", 380, false, 45),
            new Destination("budapest", "Budapest", "בודפשט", "hungary", 340, false, 45),
            new Destination("vienna", "Vienna", "וינה", "austria", 500, false, 40),
            new Destination("zurich", "Zurich", "ציריך", "switzerland", 820, false, 20),
            new Destination("batumi", "Batumi", "בטומי", "georgia", 290, true, 30),
            new Destination("tbilisi", "Tbilisi", "טביליסי", "georgia", 260, false, 30),
            new Destination("bucharest", "Bucharest", "בוקרשט", "romania", 300, false, 25),
            new Destination("sofia", "Sofia", "סופיה", "bulgaria", 260, false, 20),
            new Destination("varna", "Varna", "ורנה", "bulgaria", 280, true, 20),
            new Destination("dubrovnik", "Dubrovnik", "דוברובניק", "croatia", 560, true, 20),
            new Destination("split", "Split", "ספליט", "croatia", 470, true, 20),
            new Destination("dubai", "Dubai", "דובאי", "uae", 620, true, 50),
            new Destination("abu-dhabi", "Abu Dhabi", "אבו דאבי", "uae", 580, true, 25),
            new Destination("bangkok", "Bangkok", "בנגקוק", "thailand", 260, false, 45),
            new Destination("phuket", "Phuket", "פוקט", "thailand", 330, true, 40),
            new Destination("new-york", "New York", "ניו יורק", "usa", 900, false, 60),
            new Destination("miami", "Miami", "מיאמי", "usa", 760, true, 30));

    /** Country key → English and Hebrew names. */
    public static final Map<String, String[]> COUNTRIES = Map.ofEntries(
            Map.entry("israel", new String[]{"Israel", "ישראל"}),
            Map.entry("greece", new String[]{"Greece", "יוון"}),
            Map.entry("cyprus", new String[]{"Cyprus", "קפריסין"}),
            Map.entry("italy", new String[]{"Italy", "איטליה"}),
            Map.entry("france", new String[]{"France", "צרפת"}),
            Map.entry("spain", new String[]{"Spain", "ספרד"}),
            Map.entry("portugal", new String[]{"Portugal", "פורטוגל"}),
            Map.entry("uk", new String[]{"United Kingdom", "בריטניה"}),
            Map.entry("netherlands", new String[]{"Netherlands", "הולנד"}),
            Map.entry("germany", new String[]{"Germany", "גרמניה"}),
            Map.entry("czechia", new String[]{"Czechia", "צ׳כיה"}),
            Map.entry("hungary", new String[]{"Hungary", "הונגריה"}),
            Map.entry("austria", new String[]{"Austria", "אוסטריה"}),
            Map.entry("switzerland", new String[]{"Switzerland", "שווייץ"}),
            Map.entry("georgia", new String[]{"Georgia", "גאורגיה"}),
            Map.entry("romania", new String[]{"Romania", "רומניה"}),
            Map.entry("bulgaria", new String[]{"Bulgaria", "בולגריה"}),
            Map.entry("croatia", new String[]{"Croatia", "קרואטיה"}),
            Map.entry("uae", new String[]{"United Arab Emirates", "איחוד האמירויות"}),
            Map.entry("thailand", new String[]{"Thailand", "תאילנד"}),
            Map.entry("usa", new String[]{"United States", "ארצות הברית"}));

    /** Countries in the order they are shown, Israel first. */
    public static final List<String> COUNTRY_ORDER = List.of("israel", "greece", "cyprus", "italy", "france", "spain",
            "portugal", "uk", "netherlands", "germany", "czechia", "hungary", "austria", "switzerland", "croatia",
            "georgia", "romania", "bulgaria", "uae", "thailand", "usa");

    /** Where in town; shown translated ("area.BEACH"). */
    static final String[] CITY_AREAS = {"CENTER", "OLD_TOWN", "BUSINESS", "QUIET", "STATION", "PARK", "MARKET", "HARBOR"};
    static final String[] SEA_AREAS = {"BEACH", "PROMENADE", "MARINA", "BAY"};

    public static final int DESCRIPTIONS = 8;
    public static final String RESOURCE = "/catalog/hotels.tsv";

    /** Names that say the hotel is by the water. */
    private static final Pattern SEASIDE_NAME = Pattern.compile(
            "(?iu).*(\\bbeach|\\bsea\\b|seaside|seaview|\\bmare\\b|\\bmar\\b|plage|playa|praia|\\bbay\\b|ocean|marina|coast|shore|riviera|lagoon|חוף|(^|\\s)ים(\\s|$)|מרינה|resort|ריזורט|island).*");
    private static final Set<String> GREEK_ISLANDS = Set.of("santorini", "crete", "rhodes");
    private static final Set<String> TROPICAL = Set.of("phuket", "miami", "dubai", "abu-dhabi");
    private static final Set<String> RESORT_TOWNS = Set.of("eilat", "dead-sea", "mitzpe-ramon", "arad");

    private HotelCatalog() {
    }

    public static Optional<Destination> destination(String cityName) {
        return DESTINATIONS.stream().filter(d -> d.name().equalsIgnoreCase(cityName)).findFirst();
    }

    public static Optional<Destination> byKey(String key) {
        return DESTINATIONS.stream().filter(d -> d.key().equals(key)).findFirst();
    }

    /** The country key whose English or Hebrew name is exactly the query ("Greece", "יוון"), if any. */
    public static Optional<String> country(String query) {
        if (query == null || query.isBlank()) {
            return Optional.empty();
        }
        String q = query.trim();
        return COUNTRIES.entrySet().stream()
                .filter(e -> e.getValue()[0].equalsIgnoreCase(q) || e.getValue()[1].equals(q) || e.getKey().equalsIgnoreCase(q))
                .map(Map.Entry::getKey).findFirst();
    }

    /** Destinations whose city or country name (English or Hebrew) contains the query. */
    public static List<Destination> matching(String query) {
        if (query == null || query.isBlank()) {
            return DESTINATIONS;
        }
        String q = query.trim().toLowerCase(Locale.ROOT);
        return DESTINATIONS.stream().filter(d -> {
            String[] c = COUNTRIES.get(d.country());
            return d.matches(query) || c[0].toLowerCase(Locale.ROOT).contains(q) || c[1].contains(query.trim());
        }).toList();
    }

    public static List<Destination> inCountry(String country) {
        return DESTINATIONS.stream().filter(d -> d.country().equals(country)).toList();
    }

    /** Reads the real hotels shipped with the app. */
    public static List<Hotel> load(RoomFactory factory) {
        try (InputStream in = HotelCatalog.class.getResourceAsStream(RESOURCE)) {
            if (in == null) {
                // the hotel list hasn't been downloaded yet: run OsmImport once (see README)
                System.err.println("DiviStay: " + RESOURCE + " is missing. Run com.divistay.catalog.OsmImport once to download the hotels.");
                return List.of();
            }
            return read(new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8)), factory);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * One hotel per line, tab-separated:
     * city key, name, English name, Hebrew name, stars, latitude, longitude, street, neighbourhood.
     */
    static List<Hotel> read(BufferedReader reader, RoomFactory factory) throws IOException {
        List<Hotel> hotels = new ArrayList<>();
        Map<String, Integer> perCity = new HashMap<>();
        Set<String> seen = new java.util.HashSet<>();
        String line;
        while ((line = reader.readLine()) != null) {
            if (line.isBlank() || line.startsWith("#")) {
                continue;
            }
            String[] f = line.split("\t", -1);
            if (f.length < 9) {
                continue;
            }
            Optional<Destination> dest = byKey(f[0]);
            if (dest.isEmpty() || !looksLikeAName(f[1])) {
                continue;
            }
            // the same hotel can be in OpenStreetMap twice (as a point and as a building): keep one
            String dedupeKey = f[0] + "|" + f[1].toLowerCase(Locale.ROOT).replaceAll("(?U)\\W+", "")
                    + "|" + Math.round(Double.parseDouble(f[5]) * 300) + "|" + Math.round(Double.parseDouble(f[6]) * 300);
            if (!seen.add(dedupeKey)) {
                continue;
            }
            int n = perCity.merge(f[0], 1, Integer::sum);
            hotels.add(hotel(dest.get(), n, f, factory));
        }
        return hotels;
    }

    private static Hotel hotel(Destination dest, int number, String[] f, RoomFactory factory) {
        String id = dest.key() + "-" + number;
        String local = f[1].trim(), en = f[2].trim(), he = f[3].trim();
        String name = !en.isEmpty() ? en : local;
        String nameHe = !he.isEmpty() ? he : local;
        double lat = Double.parseDouble(f[5]), lon = Double.parseDouble(f[6]);
        String street = f[7].trim(), suburb = f[8].trim();
        String address = street.isEmpty() ? suburb : (suburb.isEmpty() ? street : street + ", " + suburb);

        // everything that is not real data comes from a random generator seeded by the hotel itself
        Random rnd = new Random((dest.key() + "|" + local + "|" + f[5]).hashCode() * 31L + 2026);
        int stars = parseStars(f[4]);
        if (stars == 0) {
            stars = new int[]{2, 3, 3, 3, 4, 4, 4, 4, 5}[rnd.nextInt(9)];
        }
        boolean seaName = SEASIDE_NAME.matcher(local + " " + en + " " + he).matches();
        PhotoSet photos;
        if (GREEK_ISLANDS.contains(dest.key())) photos = PhotoSet.GREEK;
        else if (TROPICAL.contains(dest.key()) && (seaName || "phuket".equals(dest.key()))) photos = PhotoSet.TROPICAL;
        else if (RESORT_TOWNS.contains(dest.key())) photos = seaName ? PhotoSet.SEA : PhotoSet.RESORT;
        else if (dest.seaside() && seaName) photos = PhotoSet.SEA;
        else photos = PhotoSet.CITY;
        boolean seafront = photos == PhotoSet.SEA || photos == PhotoSet.TROPICAL || photos == PhotoSet.GREEK;
        String[] areas = seafront ? SEA_AREAS : CITY_AREAS;
        String area = areas[rnd.nextInt(areas.length)];

        double rating = Math.min(9.8, 6.3 + stars * 0.45 + rnd.nextGaussian() * 0.6);
        rating = Math.round(Math.max(6.0, rating) * 10) / 10.0;
        int reviews = 20 + rnd.nextInt(60) * (6 - stars + rnd.nextInt(4)) * 7;

        Set<Amenity> amenities = EnumSet.of(Amenity.WIFI);
        if (rnd.nextInt(10) < 7) amenities.add(Amenity.BREAKFAST);
        if (photos != PhotoSet.CITY ? rnd.nextInt(5) > 0 : (stars >= 4 && rnd.nextBoolean())) amenities.add(Amenity.POOL);
        if (rnd.nextBoolean()) amenities.add(Amenity.PARKING);
        if (stars == 5 || (stars == 4 && rnd.nextBoolean())) amenities.add(Amenity.SPA);
        if (stars >= 4 && rnd.nextInt(3) > 0) amenities.add(Amenity.GYM);
        if (rnd.nextInt(4) == 0 || photos == PhotoSet.RESORT) amenities.add(Amenity.KIDS_CLUB);
        if (seafront && photos != PhotoSet.GREEK || (photos == PhotoSet.GREEK && rnd.nextBoolean())) amenities.add(Amenity.BEACH);
        if ("israel".equals(dest.country()) ? rnd.nextInt(10) < 7 : rnd.nextInt(12) == 0) amenities.add(Amenity.KOSHER);
        if (rnd.nextInt(10) < 6) amenities.add(Amenity.ACCESSIBLE);
        if (rnd.nextInt(5) == 0) amenities.add(Amenity.PETS);
        if (stars >= 3 && rnd.nextInt(3) > 0) amenities.add(Amenity.RESTAURANT);

        double factor = switch (stars) { case 1 -> 0.5; case 2 -> 0.65; case 3 -> 1.0; case 4 -> 1.45; default -> 2.2; };
        double luck = 0.82 + rnd.nextDouble() * 0.4;
        long base = Math.max(160, Math.round(dest.baseRate() * factor * luck * (seafront ? 1.15 : 1.0) / 10.0) * 10);

        List<RoomType> types = new ArrayList<>();
        int standard = 2 + rnd.nextInt(3), dbl = 1 + rnd.nextInt(3);
        int family = stars >= 3 || rnd.nextBoolean() ? 1 + rnd.nextInt(3) : 0;
        int suite = stars >= 4 ? 1 + rnd.nextInt(2) : (rnd.nextInt(4) == 0 ? 1 : 0);
        for (int i = 0; i < standard; i++) types.add(RoomType.STANDARD);
        for (int i = 0; i < dbl; i++) types.add(RoomType.DOUBLE);
        for (int i = 0; i < family; i++) types.add(RoomType.FAMILY);
        for (int i = 0; i < suite; i++) types.add(RoomType.SUITE);
        List<Room> rooms = new ArrayList<>();
        for (int i = 0; i < types.size(); i++) {
            String room = (i / 4 + 1) + String.format("%02d", i % 4 + 1);
            rooms.add(factory.create(id, room, types.get(i), Money.shekels(base)));
        }

        return new Hotel(id, name, nameHe, dest.name(), address, dest.name() + " · " + area, rooms, stars, rating, reviews,
                area, amenities, rnd.nextInt(DESCRIPTIONS), rnd.nextInt(1_000_000), photos, lat, lon, true);
    }

    /** Skips entries whose "name" is only a number or the word "hotel". */
    static boolean looksLikeAName(String name) {
        String n = name.trim();
        return n.length() >= 3 && !n.matches("(?U)[\\d\\W_]+") && !n.equalsIgnoreCase("hotel") && !n.equals("מלון");
    }

    /** "4", "4S", "3.5" → 4, 4, 3; anything else → 0 (unknown). */
    static int parseStars(String s) {
        if (s == null) return 0;
        java.util.regex.Matcher m = Pattern.compile("^\\s*([1-5])").matcher(s);
        return m.find() ? Integer.parseInt(m.group(1)) : 0;
    }
}
