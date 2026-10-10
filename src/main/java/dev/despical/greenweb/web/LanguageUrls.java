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

package dev.despical.greenweb.web;

import jakarta.servlet.http.HttpServletRequest;

import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.stream.Collectors;

/**
 * Builds local navigation targets and rejects external, encoded or malformed redirect paths.
 *
 * @author Despical
 */
public final class LanguageUrls {

    private LanguageUrls() {}

    public static String currentPage(HttpServletRequest request) {
        String query = request.getQueryString();
        return cleanReturnTo(request.getRequestURI() + (query == null ? "" : "?" + query));
    }

    public static String cleanReturnTo(String value) {
        if (value == null
                || value.length() > 4096
                || !value.startsWith("/")
                || value.startsWith("//")
                || unsafeCharacters(value)) {
            return "/";
        }
        try {
            URI uri = new URI(value);
            if (uri.isAbsolute()
                    || uri.getRawAuthority() != null
                    || uri.getRawPath() == null
                    || uri.getRawPath().equals("/language")) {
                return "/";
            }
            String decodedPath = uri.getRawPath();
            for (int pass = 0; pass < 3; pass++) {
                decodedPath =
                        URLDecoder.decode(decodedPath.replace("+", "%2B"), StandardCharsets.UTF_8);
                if (decodedPath.startsWith("//") || unsafeCharacters(decodedPath)) {
                    return "/";
                }
            }
            String query = uri.getRawQuery();
            if (query != null) {
                query =
                        Arrays.stream(query.split("&", -1))
                                .filter(part -> !isLanguageParameter(part))
                                .collect(Collectors.joining("&"));
            }
            return uri.getRawPath()
                    + (query == null || query.isEmpty() ? "" : "?" + query)
                    + (uri.getRawFragment() == null ? "" : "#" + uri.getRawFragment());
        } catch (URISyntaxException | IllegalArgumentException ignored) {
            return "/";
        }
    }

    private static boolean isLanguageParameter(String part) {
        String key = part.split("=", 2)[0];
        return URLDecoder.decode(key, StandardCharsets.UTF_8).equals("lang");
    }

    private static boolean unsafeCharacters(String value) {
        return value.chars().anyMatch(c -> c <= 0x20 || c == 0x7f || c == '\\');
    }
}
