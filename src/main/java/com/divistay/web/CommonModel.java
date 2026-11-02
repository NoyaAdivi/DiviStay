package com.divistay.web;

import com.divistay.catalog.HotelCatalog;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

/** Values every page needs: the language-switch links and the contact link. */
@ControllerAdvice
public class CommonModel {

    public static final String LINKEDIN = "https://www.linkedin.com/in/noya-adivi";

    @ModelAttribute
    public void common(HttpServletRequest request, Model model) {
        model.addAttribute("linkedin", LINKEDIN);
        model.addAttribute("destinations", HotelCatalog.DESTINATIONS);
        String base = switchBase(request);
        String sep = base.contains("?") ? "&" : "?";
        model.addAttribute("langUrlEn", base + sep + "lang=en");
        model.addAttribute("langUrlHe", base + sep + "lang=he");
    }

    /**
     * The same page without the "lang" parameter. A page shown after a form POST (for example the hotel page
     * with an error) is switched to its GET address, keeping only the dates.
     */
    static String switchBase(HttpServletRequest request) {
        String path = request.getRequestURI();
        boolean post = "POST".equalsIgnoreCase(request.getMethod());
        List<String> keepOnPost = List.of("checkIn", "checkOut");
        if (post && path.endsWith("/groups")) {
            path = path.substring(0, path.length() - "/groups".length());
        }
        StringBuilder query = new StringBuilder();
        for (Map.Entry<String, String[]> e : request.getParameterMap().entrySet()) {
            String name = e.getKey();
            if (name.equals("lang") || (post && !keepOnPost.contains(name))) {
                continue;
            }
            for (String value : e.getValue()) {
                query.append(query.isEmpty() ? "?" : "&")
                        .append(URLEncoder.encode(name, StandardCharsets.UTF_8)).append('=')
                        .append(URLEncoder.encode(value, StandardCharsets.UTF_8));
            }
        }
        return path + query;
    }
}
