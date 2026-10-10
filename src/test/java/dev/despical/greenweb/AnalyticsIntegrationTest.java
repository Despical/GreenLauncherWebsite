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
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package dev.despical.greenweb;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.file.Path;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:analytics-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "app.public-url=https://launcher.despical.dev",
            "app.google-analytics-id=G-TEST2026",
            "app.seed-releases=false",
            "app.local-admin=false",
            "app.admin-password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        })
@AutoConfigureMockMvc
class AnalyticsIntegrationTest {

    @TempDir static Path data;
    @Autowired MockMvc mvc;

    @DynamicPropertySource
    static void isolatedStorage(DynamicPropertyRegistry properties) {
        properties.add("app.storage", () -> data.toString());
    }

    @Test
    void publicHttpsPagesExposeConsentControlsWithoutLoadingGoogleTags() throws Exception {
        mvc.perform(
                        get("/features")
                                .secure(true)
                                .with(
                                        request -> {
                                            request.setServerName("launcher.despical.dev");
                                            return request;
                                        }))
                .andExpect(status().isOk())
                .andExpect(content().string(containsString("G-TEST2026")))
                .andExpect(content().string(containsString("data-analytics-accept")))
                .andExpect(content().string(containsString("data-analytics-reject")))
                .andExpect(content().string(containsString("/js/analytics.js")))
                .andExpect(content().string(not(containsString("googletagmanager.com/gtag/js"))));
    }

    @Test
    void administratorAndLocalPagesDoNotExposeAnalytics() throws Exception {
        mvc.perform(
                        get("/admin/login")
                                .secure(true)
                                .with(
                                        request -> {
                                            request.setServerName("launcher.despical.dev");
                                            return request;
                                        }))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("google-analytics-id"))))
                .andExpect(content().string(not(containsString("/js/analytics.js"))));
        mvc.perform(get("/features"))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("google-analytics-id"))));
        mvc.perform(
                        get("/features")
                                .secure(true)
                                .with(
                                        request -> {
                                            request.setServerName("untrusted.example");
                                            return request;
                                        }))
                .andExpect(status().isOk())
                .andExpect(content().string(not(containsString("google-analytics-id"))));
    }
}
