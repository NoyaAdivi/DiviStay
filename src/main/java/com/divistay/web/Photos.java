package com.divistay.web;

import com.divistay.domain.Hotel;
import com.divistay.domain.PhotoSet;
import com.divistay.domain.RoomType;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;

/**
 * Photos for hotels and room types, used as {@code ${@photos.hotel(h, 0)}}.
 * They are free photos from Unsplash (unsplash.com/license), loaded from Unsplash's image server.
 * They illustrate the kind of place (city, seaside, Greek island...), they are not photos of the real hotel.
 * Each hotel gets its own mix, chosen from its photo seed, so it always looks the same.
 * If a photo can't load, the page shows a soft pastel picture instead (see app.js).
 */
@Component("photos")
public class Photos {

    /** Outside photos, by the kind of place, so a hotel in the middle of a city never shows a tropical beach. */
    private static final Map<PhotoSet, List<String>> OUTSIDE = Map.of(
            PhotoSet.CITY, List.of("1776262438118-18e2a5613eca", "1761023890575-ad31e0bc27d4", "1654783860623-8804ecbd53af",
                    "1563802459091-a18fc00cc7e5", "1560179707-f14e90ef3623", "1519335664-9434143e2067",
                    "1520264834865-7effb972c224", "1559422042-f1ae22e982f2", "1633922830531-86ed1f231613",
                    "1785910723802-99967d4fa265", "1542314831-068cd1dbfeeb", "1629044562928-6946d8d3feea"),
            PhotoSet.SEA, List.of("1540541338287-41700207dee6", "1571003123894-1f0594d2b5d9", "1561325430-51f8495633c0",
                    "1602008194020-13ac6665ebdb", "1660074430108-8eb0d51ac629", "1603030580707-d5f787d8b1c7",
                    "1471085507142-12355181f804", "1561917657-09bd994eb04a", "1534001265532-393289eb8ed3",
                    "1716630098333-e01db95aea35"),
            PhotoSet.TROPICAL, List.of("1549294413-26f195200c16", "1610641818989-c2051b5e2cfd", "1650965171703-087486b3a1b0",
                    "1733253870394-d7feafd52891", "1551882547-ff40c63fe5fa", "1787059717832-a8f7d527d9a5",
                    "1566073771259-6a8506099945"),
            PhotoSet.GREEK, List.of("1613395877344-13d4a8e0d49e", "1580502304784-8985b7eb7260", "1570077188670-e3a8d69ac5ff",
                    "1596933144889-c15043a9ddde", "1618500031461-a5fc01e96763", "1601581875309-fafbf2d3ed3a",
                    "1563789031959-4c02bcb41319"),
            PhotoSet.RESORT, List.of("1445019980597-93fa8acb246c", "1520250497591-112f2f40a3f4", "1667987566780-3b31fa5485c8",
                    "1490465998231-e16519a88298", "1775152928511-a18ed5197827", "1566073771259-6a8506099945",
                    "1716630098333-e01db95aea35"));

    /** Photos of the cities themselves, for destination cards. */
    private static final Map<String, List<String>> CITY = Map.ofEntries(
            Map.entry("tel-aviv", List.of("1547483029-77784da27709", "1598087495082-15d7dd26cf53", "1719757633949-3a0e42706f6d")),
            Map.entry("jerusalem", List.of("1614517453351-6c1522fc7a56", "1587297069400-6cc5da81658a", "1781790235056-f01fce623a1e")),
            Map.entry("rome", List.of("1552832230-c0197dd311b5", "1555992828-ca4dbe41d294", "1515542622106-78bda8ba0e5b")),
            Map.entry("paris", List.of("1502602898657-3e91760cbb34", "1511739001486-6bfe10ce785f", "1431274172761-fca41d930114")),
            Map.entry("athens", List.of("1603565816030-6b389eeb23cb", "1555993539-1732b0258235", "1620232256209-7d23ce90c8a4")),
            Map.entry("barcelona", List.of("1583422409516-2895a77efded", "1579282240050-352db0a14c21", "1534001265532-393289eb8ed3")),
            Map.entry("madrid", List.of("1539037116277-4db20889f2d4")),
            Map.entry("london", List.of("1513635269975-59663e0ac1ad", "1486299267070-83823f5448dd", "1533929736458-ca588d08c8be")),
            Map.entry("dubai", List.of("1512453979798-5ea266f8880c", "1518684079-3c830dcef090", "1546412414-e1885259563a")),
            Map.entry("new-york", List.of("1496588152823-86ff7695e68f", "1499092346589-b9b6be3e94b2", "1543716091-a840c05249ec")),
            Map.entry("prague", List.of("1600623471616-8c1966c91ff6", "1571778200037-250828fe3eee", "1564511287568-54483b52a35e")),
            Map.entry("santorini", List.of("1533105079780-92b9be482077", "1570077188670-e3a8d69ac5ff", "1580502304784-8985b7eb7260")),
            Map.entry("crete", List.of("1664118145742-f2ad1c09c10b", "1557353212-adb233ad2181")),
            Map.entry("rhodes", List.of("1616763866684-b215941fae5e", "1602008194020-13ac6665ebdb")));

    /** Free photos of the other destinations, from Wikimedia Commons (credits in docs/PHOTO_CREDITS.md). */
    private static final Map<String, String> COMMONS = loadCommons();

    private static Map<String, String> loadCommons() {
        Properties file = new Properties();
        try (InputStream in = Photos.class.getResourceAsStream("/catalog/city-photos.properties")) {
            if (in != null) {
                file.load(in);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        Map<String, String> urls = new HashMap<>();
        file.stringPropertyNames().forEach(key -> urls.put(key, file.getProperty(key)));
        return urls;
    }

    /** Inside the hotel: lobby, lounge, pool bar. */
    private static final List<String> INSIDE = List.of(
            "1660557989695-14fac79c086d", "1758448656987-cfae6bf225e4", "1590447158019-883d8d5f8bc7",
            "1505410603994-c3ac6269711f", "1564078516393-cf04bd966897", "1660557989725-f511e9fa6267",
            "1760926455212-d42b180ad602", "1763380631462-12a2f528ff25");

    private static final Map<RoomType, List<String>> ROOMS = Map.of(
            RoomType.STANDARD, List.of("1646148707098-a27b818d5c95", "1646572961739-58557003b037",
                    "1647705722868-3ff8e93b73b2", "1649265825072-f7dd6942baed", "1658595148963-13b7da6dcd6d",
                    "1595526051245-4506e0005bd0", "1615529162924-f8605388461d", "1642541070065-3912f347e7c6"),
            RoomType.DOUBLE, List.of("1631844820835-e698dc518bc6", "1683237854712-adb97a9eaa80",
                    "1744187170989-0da1811af435", "1657639754502-3c138cb24b4c", "1790774883982-7136ce871bc5",
                    "1785292446617-6dcec9c76dd5"),
            RoomType.FAMILY, List.of("1781415980730-bfcf192e38bc", "1774556743949-f4734bd8393c",
                    "1636917754428-60ece5e521df", "1780042731951-3885599aafa9", "1790774888379-dd545dfa3343",
                    "1578683010236-d716f9a3f461"),
            RoomType.SUITE, List.of("1616594039964-ae9021a400a0", "1765862835193-3c37388a409e",
                    "1646936345966-273c8cd3ce83", "1646743742987-cf69915d21cf", "1660557989725-f511e9fa6267",
                    "1560185893-a55cbc8c57e8"));

    static String url(String id, int width) {
        return "https://images.unsplash.com/photo-" + id + "?auto=format&fit=crop&w=" + width + "&q=70";
    }

    /** The i-th photo for this seed; consecutive i never repeat a photo within a list. */
    private static String pick(List<String> list, int seed, int i) {
        return list.get(Math.floorMod(seed + i, list.size()));
    }

    private static List<String> outside(Hotel hotel) {
        return OUTSIDE.get(hotel.photoSet());
    }

    /** The hotel's i-th outside photo (0 is the cover). */
    public String hotel(Hotel hotel, int i) {
        return url(pick(outside(hotel), hotel.photoSeed(), i), 900);
    }

    public String thumb(Hotel hotel, int i) {
        return url(pick(outside(hotel), hotel.photoSeed(), i), 480);
    }

    public boolean hasCity(String key) {
        return CITY.containsKey(key) || COMMONS.containsKey(key);
    }

    /** A photo of the city itself; for a city without one, a photo that fits the kind of place. */
    public String city(String key, boolean big) {
        String commons = COMMONS.get(key);
        if (commons != null) {
            return big ? commons.replace("/500px-", "/960px-") : commons;
        }
        List<String> list = CITY.get(key);
        if (list == null) {
            list = OUTSIDE.get(PhotoSet.SEA);
        }
        return url(list.get(Math.floorMod(key.hashCode(), list.size())), big ? 1100 : 600);
    }

    /** The gallery on the hotel page: three outside photos, two inside. */
    public List<String> gallery(Hotel hotel) {
        List<String> g = new ArrayList<>();
        List<String> out = outside(hotel);
        g.add(url(pick(out, hotel.photoSeed(), 0), 1400));
        g.add(url(pick(INSIDE, hotel.photoSeed(), 0), 900));
        g.add(url(pick(out, hotel.photoSeed(), 1), 900));
        g.add(url(pick(INSIDE, hotel.photoSeed(), 1), 900));
        g.add(url(pick(out, hotel.photoSeed(), 2), 900));
        return g;
    }

    /** Photos of one room type in this hotel. */
    public List<String> room(Hotel hotel, RoomType type) {
        List<String> list = ROOMS.get(type);
        List<String> out = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            out.add(url(pick(list, hotel.photoSeed() / 3 + type.ordinal(), i), 720));
        }
        return out;
    }
}
