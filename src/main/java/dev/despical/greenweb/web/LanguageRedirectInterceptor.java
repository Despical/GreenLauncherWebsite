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

import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.support.RequestContextUtils;

/**
 * Migrates legacy language-query bookmarks to clean URLs while saving their language choice.
 *
 * @author Despical
 */
public final class LanguageRedirectInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(
            HttpServletRequest request, HttpServletResponse response, Object handler) {
        if (!request.getMethod().equals("GET") || !request.getParameterMap().containsKey("lang")) {
            return true;
        }
        SiteLocaleResolver.supported(request.getParameter("lang"))
                .ifPresent(
                        locale ->
                                RequestContextUtils.getLocaleResolver(request)
                                        .setLocale(request, response, locale));
        response.setStatus(HttpServletResponse.SC_FOUND);
        response.setHeader("Location", LanguageUrls.currentPage(request));
        return false;
    }
}
