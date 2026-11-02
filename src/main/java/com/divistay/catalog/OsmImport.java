package com.divistay.catalog;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.PrintWriter;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

/**
 * A small tool (not part of the website) that downloads the real hotels of every destination from
 * OpenStreetMap, through the Overpass API, into {@code src/main/resources/catalog/hotels.tsv}.
 * Run it once from IntelliJ (the green arrow next to {@code main}); it takes a few minutes.
 * Hotel data © OpenStreetMap contributors, ODbL.
 */
public class OsmImport {

    /** city key, centre latitude, centre longitude, half-size of the box in degrees */
    private static final Object[][] AREAS = {
            {"tel-aviv", 32.08, 34.78, .07}, {"jerusalem", 31.78, 35.21, .08}, {"eilat", 29.55, 34.95, .06},
            {"haifa", 32.80, 35.00, .07}, {"dead-sea", 31.20, 35.37, .12}, {"tiberias", 32.79, 35.53, .05},
            {"netanya", 32.32, 34.86, .05}, {"herzliya", 32.17, 34.80, .04}, {"nazareth", 32.70, 35.30, .04},
            {"akko", 32.93, 35.08, .03}, {"safed", 32.965, 35.50, .03}, {"caesarea", 32.50, 34.90, .03},
            {"mitzpe-ramon", 30.61, 34.80, .04}, {"ashkelon", 31.67, 34.57, .05}, {"ashdod", 31.80, 34.65, .05},
            {"beersheba", 31.25, 34.79, .06}, {"rosh-pina", 32.97, 35.54, .03}, {"zichron-yaakov", 32.57, 34.95, .03},
            {"bat-yam", 32.02, 34.75, .025}, {"nahariya", 33.01, 35.10, .03}, {"ramat-gan", 32.08, 34.82, .02},
            {"arad", 31.26, 35.21, .04}, {"athens", 37.98, 23.73, .12}, {"rhodes", 36.25, 27.95, .35},
            {"crete", 35.25, 24.9, .45, 1.82}, {"santorini", 36.40, 25.43, .12}, {"thessaloniki", 40.62, 22.96, .1},
            {"larnaca", 34.92, 33.62, .08}, {"paphos", 34.78, 32.42, .08}, {"limassol", 34.69, 33.03, .08},
            {"rome", 41.90, 12.49, .12}, {"milan", 45.46, 9.19, .1}, {"venice", 45.44, 12.33, .08},
            {"florence", 43.77, 11.25, .07}, {"paris", 48.86, 2.35, .1}, {"nice", 43.70, 7.27, .06},
            {"barcelona", 41.39, 2.17, .09}, {"madrid", 40.42, -3.70, .1}, {"lisbon", 38.72, -9.14, .08},
            {"london", 51.51, -0.12, .14}, {"amsterdam", 52.37, 4.90, .08}, {"berlin", 52.52, 13.40, .12},
            {"munich", 48.14, 11.58, .09}, {"prague", 50.08, 14.43, .09}, {"budapest", 47.50, 19.05, .09},
            {"vienna", 48.21, 16.37, .09}, {"zurich", 47.37, 8.54, .06}, {"batumi", 41.63, 41.63, .06},
            {"tbilisi", 41.72, 44.79, .09}, {"bucharest", 44.43, 26.10, .09}, {"sofia", 42.70, 23.32, .08},
            {"varna", 43.21, 27.92, .08}, {"dubrovnik", 42.65, 18.09, .06}, {"split", 43.51, 16.44, .06},
            {"dubai", 25.15, 55.25, .18}, {"abu-dhabi", 24.45, 54.40, .15}, {"bangkok", 13.75, 100.53, .15},
            {"phuket", 7.95, 98.33, .25}, {"new-york", 40.74, -73.98, .12}, {"miami", 25.79, -80.18, .1}};

    private static final String[] SERVERS = {
            "https://overpass-api.de/api/interpreter", "https://overpass.private.coffee/api/interpreter"};

    public static void main(String[] args) throws Exception {
        Path out = Path.of(args.length > 0 ? args[0] : "src/main/resources/catalog/hotels.tsv");
        Path log = Path.of("osm-import.log");
        HttpClient http = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(30)).build();
        ObjectMapper json = new ObjectMapper();
        ConcurrentHashMap<String, List<String>> rows = new ConcurrentHashMap<>();
        List<String> logLines = java.util.Collections.synchronizedList(new ArrayList<>());

        // one worker per server; each takes the next city that nobody has started yet
        java.util.concurrent.ConcurrentLinkedQueue<Object[]> queue = new java.util.concurrent.ConcurrentLinkedQueue<>(List.of(AREAS));
        ExecutorService pool = Executors.newFixedThreadPool(SERVERS.length * 2);
        for (int w = 0; w < SERVERS.length * 2; w++) {
            String server = SERVERS[w % SERVERS.length];
            pool.submit(() -> {
                Object[] a;
                while ((a = queue.poll()) != null) {
                    String key = (String) a[0];
                    double lat = (double) a[1], lon = (double) a[2], r = (double) a[3];
                    double rLon = a.length > 4 ? (double) a[4] : r * 1.3;
                    String q = String.format(Locale.ROOT,
                            "[out:json][timeout:180];nwr[\"tourism\"=\"hotel\"][\"name\"](%.3f,%.3f,%.3f,%.3f);out center tags qt;",
                            lat - r, lon - rLon, lat + r, lon + rLon);
                    for (int attempt = 1; attempt <= 6; attempt++) {
                        String use = attempt % 2 == 1 ? server : SERVERS[(java.util.Arrays.asList(SERVERS).indexOf(server) + 1) % SERVERS.length];
                        try {
                            HttpRequest req = HttpRequest.newBuilder(URI.create(use))
                                    .timeout(Duration.ofMinutes(4))
                                    .header("Content-Type", "application/x-www-form-urlencoded")
                                    .header("User-Agent", "DiviStay student project (OSM hotel import)")
                                    .POST(HttpRequest.BodyPublishers.ofString("data=" + URLEncoder.encode(q, StandardCharsets.UTF_8)))
                                    .build();
                            HttpResponse<String> res = http.send(req, HttpResponse.BodyHandlers.ofString());
                            if (res.statusCode() != 200) {
                                throw new IOException("HTTP " + res.statusCode());
                            }
                            List<String> list = new ArrayList<>();
                            for (JsonNode e : json.readTree(res.body()).path("elements")) {
                                JsonNode t = e.path("tags");
                                double la = e.has("lat") ? e.path("lat").asDouble() : e.path("center").path("lat").asDouble();
                                double lo = e.has("lon") ? e.path("lon").asDouble() : e.path("center").path("lon").asDouble();
                                if (outsideCity(key, la, lo)) continue;
                                String suburb = t.path("addr:suburb").asText(t.path("addr:city_district").asText(""));
                                list.add(String.join("\t", key, clean(t.path("name").asText("")), clean(t.path("name:en").asText("")),
                                        clean(t.path("name:he").asText("")), clean(t.path("stars").asText("")),
                                        String.format(Locale.ROOT, "%.5f", la), String.format(Locale.ROOT, "%.5f", lo),
                                        clean(t.path("addr:street").asText("")), clean(suburb)));
                            }
                            rows.put(key, list);
                            logLines.add(key + " " + list.size());
                            System.out.println(key + ": " + list.size() + " hotels");
                            break;
                        } catch (Exception ex) {
                            logLines.add(key + " attempt " + attempt + " failed: " + ex.getMessage());
                            System.out.println(key + ": attempt " + attempt + " failed (" + ex.getMessage() + "), trying again");
                            sleep(15_000L * attempt);
                        }
                    }
                    write(out, log, rows, logLines);
                    sleep(3_000);
                }
            });
        }
        pool.shutdown();
        pool.awaitTermination(3, TimeUnit.HOURS);
        write(out, log, rows, logLines);
        logLines.add("DONE");
        write(out, log, rows, logLines);
        System.out.println("Done: " + rows.values().stream().mapToInt(List::size).sum() + " hotels in " + out.toAbsolutePath());
    }

    /** The search boxes are rectangles, so some reach over a border or into the next city. */
    static boolean outsideCity(String key, double lat, double lon) {
        return switch (key) {
            case "eilat" -> lon >= 34.975 || lat < 29.493; // Aqaba (Jordan) and Taba (Egypt)
            case "jerusalem" -> lat < 31.725; // Bethlehem and Beit Jala
            default -> false;
        };
    }

    private static synchronized void write(Path out, Path log, ConcurrentHashMap<String, List<String>> rows, List<String> logLines) {
        try {
            Files.createDirectories(out.getParent());
            try (PrintWriter w = new PrintWriter(Files.newBufferedWriter(out, StandardCharsets.UTF_8))) {
                w.println("# DiviStay hotels: real hotels from OpenStreetMap (tourism=hotel). © OpenStreetMap contributors, ODbL 1.0.");
                w.println("# city\tname\tname:en\tname:he\tstars\tlat\tlon\tstreet\tsuburb");
                for (Object[] a : AREAS) {
                    for (String line : rows.getOrDefault((String) a[0], List.of())) {
                        w.println(line);
                    }
                }
            }
            synchronized (logLines) {
                Files.write(log, new ArrayList<>(logLines), StandardCharsets.UTF_8);
            }
        } catch (IOException e) {
            System.out.println("Could not write: " + e.getMessage());
        }
    }

    private static String clean(String s) {
        return s.replaceAll("[\\t\\r\\n]+", " ").trim();
    }

    private static void sleep(long ms) {
        try {
            Thread.sleep(ms);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
}
