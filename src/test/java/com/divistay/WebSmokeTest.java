package com.divistay;

import com.divistay.domain.BookingGroup;
import com.divistay.domain.Hotel;
import com.divistay.repo.HotelRepository;
import com.divistay.repo.BookingGroupRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Boots the whole application and walks through the pages a user would see:
 * home → search → hotel → create group → dashboard → payment page → pay.
 * This catches template mistakes that the plain-Java tests can't.
 */
@SpringBootTest
@AutoConfigureMockMvc
class WebSmokeTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    BookingGroupRepository groups;

    @Autowired
    HotelRepository hotels;

    /** A real hotel in that city with a plain name (no characters that HTML would escape). */
    private Hotel hotelIn(String city) {
        return hotels.findByCity(city).stream().filter(h -> h.name().matches("[A-Za-z0-9 ]{4,}")).findFirst().orElseThrow();
    }

    private final String checkIn = LocalDate.now().plusDays(30).toString();
    private final String checkOut = LocalDate.now().plusDays(32).toString();

    @Test
    void homeAndSearchPagesRender() throws Exception {
        assertTrue(hotels.findAll().size() >= 10_000, "at least 10,000 real hotels");
        mvc.perform(get("/")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Popular with groups")));
        Hotel eilat = hotelIn("Eilat");
        mvc.perform(get("/search").param("city", "Eilat").param("checkIn", checkIn).param("checkOut", checkOut)
                        .param("sort", "PRICE").param("adults", "1"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("OpenStreetMap")));
        mvc.perform(get("/hotels/" + eilat.id()).param("checkIn", checkIn).param("checkOut", checkOut))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(eilat.name())))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Choose rooms")));
        mvc.perform(get("/destinations")).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Greece")));
        // searching for a country finds the hotels of all its cities
        mvc.perform(get("/search").param("city", "יוון"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Greece")));
    }

    @Test
    void fullGroupFlowThroughTheWebPages() throws Exception {
        Hotel haifa = hotelIn("Haifa");
        MvcResult created = mvc.perform(post("/hotels/" + haifa.id() + "/groups")
                        .param("checkIn", checkIn).param("checkOut", checkOut)
                        .param("organizerName", "Noya").param("organizerEmail", "noya@example.com")
                        .param("roomIds", haifa.rooms().get(0).id())
                        .param("splitType", "EQUAL")
                        .param("members[0].name", "Noya").param("members[0].email", "noya@example.com")
                        .param("members[1].name", "Amit").param("members[1].email", "amit@example.com"))
                .andExpect(status().is3xxRedirection())
                .andReturn();
        String dashboardUrl = created.getResponse().getRedirectedUrl();
        assertTrue(dashboardUrl != null && dashboardUrl.startsWith("/groups/"), "redirects to the dashboard");

        mvc.perform(get(dashboardUrl.replace("?created", ""))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Collecting payments")));

        // find the payment links in the demo outbox shown on the dashboard
        String html = mvc.perform(get(dashboardUrl.replace("?created", ""))).andReturn().getResponse().getContentAsString();
        java.util.regex.Matcher m = java.util.regex.Pattern.compile("/pay/([A-Za-z0-9_-]{43})").matcher(html);
        java.util.Set<String> tokens = new java.util.LinkedHashSet<>();
        while (m.find()) {
            tokens.add(m.group(1));
        }
        assertEquals(2, tokens.size(), "one payment link per member");

        for (String token : tokens) {
            mvc.perform(get("/pay/" + token)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("Pay with card")));
            mvc.perform(post("/pay/" + token)
                            .param("holderName", "Test").param("cardNumber", "4242 4242 4242 4242")
                            .param("expiryMonth", "12").param("expiryYear", "2035").param("cvc", "123"))
                    .andExpect(status().is3xxRedirection());
            mvc.perform(get("/pay/" + token)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("all set")));
        }

        BookingGroup group = groups.findAll().stream()
                .filter(g -> g.organizerEmail().equals("noya@example.com") && g.hotelId().equals(haifa.id()))
                .findFirst().orElseThrow();
        assertEquals("CONFIRMED", group.status().name());
        mvc.perform(get(dashboardUrl.replace("?created", ""))).andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Confirmed by the hotel")));
    }

    @Test
    void invalidLinksShowAFriendlyPage() throws Exception {
        mvc.perform(get("/pay/nonsense")).andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("not valid")));
    }

    @Test
    void hebrewIsChosenFromTheBrowserAndRendersRightToLeft() throws Exception {
        mvc.perform(get("/").header("Accept-Language", "he-IL,he;q=0.9,en;q=0.8"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("dir=\"rtl\"")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("מזמינים חדרים כקבוצה")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("רומא")));
    }

    @Test
    void languageCanBeSwitchedWithAParameter() throws Exception {
        mvc.perform(get("/").param("lang", "he"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("lang=\"he\"")))
                .andExpect(org.springframework.test.web.servlet.result.MockMvcResultMatchers.cookie().value("divistay-lang", "he"));
        mvc.perform(get("/").param("lang", "en").header("Accept-Language", "he"))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Book rooms as a group.")));
    }

    @Test
    void errorsFromTheCoreAreTranslated() throws Exception {
        mvc.perform(get("/pay/nonsense").param("lang", "he"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("קישור התשלום לא תקין")));
    }

    @Test
    void legalPagesRenderInBothLanguages() throws Exception {
        for (String page : new String[]{"/privacy", "/terms", "/accessibility"}) {
            mvc.perform(get(page)).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("linkedin.com/in/noya-adivi")));
            mvc.perform(get(page).param("lang", "he")).andExpect(status().isOk())
                    .andExpect(content().string(org.hamcrest.Matchers.containsString("dir=\"rtl\"")));
        }
        mvc.perform(get("/accessibility")).andExpect(content().string(org.hamcrest.Matchers.containsString("WCAG 2.1")));
    }

    @Test
    void searchWithAGroupShowsPhotosFiltersAndWhatEachPayerPays() throws Exception {
        mvc.perform(get("/search").param("city", "רומא").param("checkIn", checkIn).param("checkOut", checkOut)
                        .param("adults", "4").param("ages", "6").param("ages", "1").param("payers", "4")
                        .param("stars", "4").param("stars", "5").param("sort", "PRICE"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("images.unsplash.com")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("Lowest price")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("split between 4")));
        mvc.perform(get("/hotels/" + hotelIn("Tel Aviv").id()).param("checkIn", checkIn).param("checkOut", checkOut)
                        .param("adults", "3").param("payers", "3"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString("How many people are paying")))
                .andExpect(content().string(org.hamcrest.Matchers.containsString("name=\"people\" id=\"peopleInput\" value=\"3\"")));
    }
}
