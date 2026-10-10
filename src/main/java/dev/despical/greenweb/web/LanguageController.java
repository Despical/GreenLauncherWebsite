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

import dev.despical.greenweb.config.SiteLocaleResolver;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.support.RequestContextUtils;

import java.util.Locale;

/**
 * Persists an explicit supported language and redirects to a validated local return path.
 *
 * @author Despical
 */
@Controller
public final class LanguageController {

    @PostMapping("/language")
    void select(
            @RequestParam String language,
            @RequestParam(defaultValue = "/") String returnTo,
            HttpServletRequest request,
            HttpServletResponse response) {
        Locale locale =
                SiteLocaleResolver.supported(language)
                        .orElseThrow(
                                () ->
                                        new ResponseStatusException(
                                                HttpStatus.BAD_REQUEST, "Unsupported language."));
        RequestContextUtils.getLocaleResolver(request).setLocale(request, response, locale);
        response.setStatus(HttpServletResponse.SC_SEE_OTHER);
        response.setHeader("Location", LanguageUrls.cleanReturnTo(returnTo));
    }
}
