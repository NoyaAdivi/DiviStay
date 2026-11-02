package com.divistay.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.i18n.CookieLocaleResolver;
import org.springframework.web.servlet.i18n.LocaleChangeInterceptor;

import java.time.Duration;
import java.util.Locale;

/**
 * English and Hebrew.
 * <ul>
 *   <li>First visit: the browser's language decides (Hebrew browsers get Hebrew, everyone else English).</li>
 *   <li>{@code ?lang=he} / {@code ?lang=en} switches and remembers the choice in a cookie for a year.</li>
 * </ul>
 * Texts live in {@code messages.properties} (English) and {@code messages_he.properties} (Hebrew).
 */
@Configuration
public class I18nConfig implements WebMvcConfigurer {

    public static final Locale HEBREW = Locale.forLanguageTag("he");

    @Bean
    public LocaleResolver localeResolver() {
        CookieLocaleResolver resolver = new CookieLocaleResolver("divistay-lang");
        resolver.setCookieMaxAge(Duration.ofDays(365));
        resolver.setDefaultLocaleFunction(I18nConfig::fromBrowser);
        return resolver;
    }

    static Locale fromBrowser(HttpServletRequest request) {
        String header = request.getHeader("Accept-Language");
        if (header != null) {
            String first = header.split(",")[0].trim().toLowerCase(Locale.ROOT);
            if (first.startsWith("he") || first.startsWith("iw")) {
                return HEBREW;
            }
        }
        return Locale.ENGLISH;
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        LocaleChangeInterceptor change = new LocaleChangeInterceptor();
        change.setParamName("lang");
        change.setIgnoreInvalidLocale(true);
        registry.addInterceptor(change);
    }
}
