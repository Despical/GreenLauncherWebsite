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

import org.springframework.beans.factory.annotation.Value;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

import java.net.URI;

/**
 * Supplies canonical URLs and social preview metadata without trusting the request Host header.
 * Administrator and error pages are excluded from search indexing.
 *
 * @author Despical
 */
@ControllerAdvice
public final class SiteMetadataAdvice {

    private final String publicUrl;

    public SiteMetadataAdvice(@Value("${app.public-url}") String publicUrl) {
        URI origin = URI.create(publicUrl);
        if (!("https".equals(origin.getScheme()) || "http".equals(origin.getScheme()))
                || origin.getHost() == null
                || origin.getUserInfo() != null
                || origin.getQuery() != null
                || origin.getFragment() != null
                || !(origin.getPath().isEmpty() || "/".equals(origin.getPath()))) {
            throw new IllegalArgumentException("SITE_PUBLIC_URL must be an HTTP(S) origin.");
        }
        this.publicUrl = publicUrl.replaceFirst("/+$", "");
    }

    @ModelAttribute
    void metadata(Model model, HttpServletRequest request) {
        String path = request.getRequestURI();
        model.addAttribute("canonicalUrl", publicUrl + path);
        model.addAttribute("socialImageUrl", publicUrl + "/images/social-cover.png");
        model.addAttribute("privatePage", path.startsWith("/admin") || path.equals("/error"));
    }
}
