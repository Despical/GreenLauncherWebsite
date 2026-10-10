/*
 * Green Launcher Website - Public downloads and release management.
 * Copyright (C) 2026  Berke Akçen
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package dev.despical.greenweb.config;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.web.servlet.LocaleResolver;
import org.springframework.web.util.WebUtils;

import java.time.Duration;
import java.util.List;
import java.util.Locale;
import java.util.Optional;

/**
 * Negotiates a supported browser language and persists explicit choices in a secure, HttpOnly
 * cookie. The locale is cached per request so every template observes the same selection.
 *
 * @author Despical
 */
public final class SiteLocaleResolver implements LocaleResolver {

    public static final String COOKIE_NAME = "site-language";

    private static final String LOCALE_ATTRIBUTE = SiteLocaleResolver.class.getName() + ".locale";

    public static final List<Locale> SUPPORTED =
            List.of("tr", "en", "de", "fr", "ru", "pl").stream()
                    .map(Locale::forLanguageTag)
                    .toList();

    public static Optional<Locale> supported(String code) {
        if (code == null) {
            return Optional.empty();
        }
        return SUPPORTED.stream()
                .filter(locale -> locale.getLanguage().equalsIgnoreCase(code))
                .findFirst();
    }

    @Override
    public Locale resolveLocale(HttpServletRequest request) {
        if (request.getAttribute(LOCALE_ATTRIBUTE) instanceof Locale locale) {
            return locale;
        }
        Cookie cookie = WebUtils.getCookie(request, COOKIE_NAME);
        Locale locale =
                cookie == null
                        ? browserLocale(request)
                        : supported(cookie.getValue()).orElseGet(() -> browserLocale(request));
        request.setAttribute(LOCALE_ATTRIBUTE, locale);
        return locale;
    }

    private Locale browserLocale(HttpServletRequest request) {
        String header = request.getHeader(HttpHeaders.ACCEPT_LANGUAGE);
        if (header != null && !header.isBlank()) {
            try {
                Locale matched = Locale.lookup(Locale.LanguageRange.parse(header), SUPPORTED);
                if (matched != null) {
                    return matched;
                }
            } catch (IllegalArgumentException ignored) {
                // A malformed client header should still render a supported language.
            }
        }
        return Locale.ENGLISH;
    }

    @Override
    public void setLocale(HttpServletRequest request, HttpServletResponse response, Locale locale) {
        if (response == null) {
            throw new IllegalArgumentException("A response is required to save the language.");
        }
        Locale choice =
                locale == null
                        ? null
                        : supported(locale.getLanguage())
                                .orElseThrow(
                                        () ->
                                                new IllegalArgumentException(
                                                        "Unsupported language."));
        if (choice == null) {
            request.removeAttribute(LOCALE_ATTRIBUTE);
        } else {
            request.setAttribute(LOCALE_ATTRIBUTE, choice);
        }
        response.addHeader(
                HttpHeaders.SET_COOKIE,
                ResponseCookie.from(COOKIE_NAME, choice == null ? "" : choice.getLanguage())
                        .path(request.getContextPath().isEmpty() ? "/" : request.getContextPath())
                        .maxAge(choice == null ? Duration.ZERO : Duration.ofDays(365))
                        .httpOnly(true)
                        .secure(request.isSecure())
                        .sameSite("Lax")
                        .build()
                        .toString());
    }
}
