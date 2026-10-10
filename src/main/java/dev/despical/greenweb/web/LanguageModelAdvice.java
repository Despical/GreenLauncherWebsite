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

import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.util.Locale;
import java.util.Map;

/**
 * Provides translated language names, flag identifiers and clean return URLs to templates.
 *
 * @author Despical
 */
@ControllerAdvice
public final class LanguageModelAdvice {

    private static final Map<String, String> FLAGS =
            Map.of("tr", "tr", "en", "gb", "de", "de", "fr", "fr", "ru", "ru", "pl", "pl");

    public record LanguageOption(String code, String displayName, String flag) {}

    @ModelAttribute
    void languages(Model model, Locale locale, HttpServletRequest request) {
        model.addAttribute(
                "languageOptions",
                SiteLocaleResolver.SUPPORTED.stream()
                        .map(
                                option -> {
                                    String name = option.getDisplayLanguage(locale);
                                    name =
                                            name.substring(0, 1).toUpperCase(locale)
                                                    + name.substring(1);
                                    return new LanguageOption(
                                            option.getLanguage(),
                                            name,
                                            FLAGS.get(option.getLanguage()));
                                })
                        .toList());
        model.addAttribute("currentLanguageCode", locale.getLanguage());
        model.addAttribute("languageReturnTo", LanguageUrls.currentPage(request));
    }
}
