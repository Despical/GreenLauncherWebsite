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

package dev.despical.greenweb;

import static org.assertj.core.api.Assertions.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import dev.despical.greenweb.config.SiteLocaleResolver;
import dev.despical.greenweb.release.*;
import dev.despical.greenweb.web.LanguageModelAdvice.LanguageOption;

import jakarta.servlet.http.Cookie;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.mock.web.MockCookie;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.net.URI;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.util.HexFormat;
import java.util.List;

@SpringBootTest(
        properties = {
            "spring.datasource.url=jdbc:h2:mem:web-test;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
            "app.seed-releases=true",
            "app.local-admin=false",
            "app.admin-password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy"
        })
@AutoConfigureMockMvc
class SiteIntegrationTest {

    @TempDir static Path data;

    @DynamicPropertySource
    static void isolatedStorage(DynamicPropertyRegistry properties) {
        properties.add("app.storage", () -> data.toString());
    }

    @Autowired MockMvc mvc;

    @Autowired ReleaseService releases;

    @Autowired ReleaseRepository repository;

    @Autowired AssetStorage storage;

    @Test
    void publicPagesRenderInEveryLanguage() throws Exception {
        for (String language : new String[] {"tr", "en", "de", "fr", "ru", "pl"}) {
            for (String path :
                    new String[] {
                        "/", "/features", "/faq", "/download", "/changelog", "/changelog/0.17.7"
                    }) {
                mvc.perform(get(path).cookie(new Cookie(SiteLocaleResolver.COOKIE_NAME, language)))
                        .andExpect(status().isOk())
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.containsString(
                                                        "lang=\"" + language + "\"")))
                        .andExpect(
                                content()
                                        .string(
                                                org.hamcrest.Matchers.not(
                                                        org.hamcrest.Matchers.containsString(
                                                                "??"))));
            }
        }
        mvc.perform(get("/admin/login")).andExpect(status().isOk());
    }

    @Test
    void publicMetadataUsesTheConfiguredOriginAndPrivatePagesAreNotIndexed() throws Exception {
        mvc.perform(head("/features")).andExpect(status().isOk());
        mvc.perform(head("/robots.txt")).andExpect(status().isOk());
        mvc.perform(head("/admin")).andExpect(status().isFound());
        mvc.perform(get("/features").header("Host", "untrusted.example"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "href=\"http://localhost:8087/features\"")))
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "content=\"http://localhost:8087/images/social-cover.png\"")));
        mvc.perform(get("/admin/login"))
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("noindex, nofollow")));
        mvc.perform(get("/robots.txt"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(org.hamcrest.Matchers.containsString("Disallow: /admin")));
        mvc.perform(get("/sitemap.xml"))
                .andExpect(status().isOk())
                .andExpect(
                        content()
                                .string(
                                        org.hamcrest.Matchers.containsString(
                                                "https://launcher.despical.dev/features")));
    }

    @Test
    void firstVisitNegotiatesBrowserLanguageAndFallsBackToEnglish() throws Exception {
        mvc.perform(get("/").header("Accept-Language", "es-MX,fr-CA;q=0.9,tr;q=0.5"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentLanguageCode", "fr"));
        mvc.perform(get("/").header("Accept-Language", "tr-TR,tr;q=0.9,en;q=0.7"))
                .andExpect(model().attribute("currentLanguageCode", "tr"));
        mvc.perform(get("/").header("Accept-Language", "ja-JP,es;q=0.8"))
                .andExpect(model().attribute("currentLanguageCode", "en"));
        mvc.perform(get("/").header("Accept-Language", "tr;q=0,en;q=0.8"))
                .andExpect(model().attribute("currentLanguageCode", "en"));
        mvc.perform(get("/")).andExpect(model().attribute("currentLanguageCode", "en"));
        mvc.perform(get("/").header("Accept-Language", "broken;q=not-a-number"))
                .andExpect(model().attribute("currentLanguageCode", "en"));
    }

    @Test
    void cookieSelectionPersistsAndOverridesTheBrowserWithLocalizedMenuNames() throws Exception {
        var selection =
                mvc.perform(
                                post("/language")
                                        .with(csrf())
                                        .param("language", "en")
                                        .param(
                                                "returnTo",
                                                "/download?art=populated&lang=tr#packages"))
                        .andExpect(status().isSeeOther())
                        .andExpect(redirectedUrl("/download?art=populated#packages"))
                        .andExpect(
                                header().string(
                                                "Set-Cookie",
                                                org.hamcrest.Matchers.allOf(
                                                        org.hamcrest.Matchers.containsString(
                                                                "site-language=en"),
                                                        org.hamcrest.Matchers.containsString(
                                                                "Path=/"),
                                                        org.hamcrest.Matchers.containsString(
                                                                "Max-Age=31536000"),
                                                        org.hamcrest.Matchers.containsString(
                                                                "HttpOnly"),
                                                        org.hamcrest.Matchers.containsString(
                                                                "SameSite=Lax"))))
                        .andReturn();
        Cookie cookie = MockCookie.parse(selection.getResponse().getHeader("Set-Cookie"));
        var page =
                mvc.perform(get("/download").cookie(cookie).header("Accept-Language", "tr-TR"))
                        .andExpect(status().isOk())
                        .andExpect(model().attribute("currentLanguageCode", "en"))
                        .andExpect(model().attribute("languageReturnTo", "/download"))
                        .andReturn();
        @SuppressWarnings("unchecked")
        List<LanguageOption> options =
                (List<LanguageOption>) page.getModelAndView().getModel().get("languageOptions");
        assertThat(options)
                .extracting(LanguageOption::displayName)
                .containsExactly("Turkish", "English", "German", "French", "Russian", "Polish");
        assertThat(options)
                .extracting(LanguageOption::flag)
                .containsExactly("tr", "gb", "de", "fr", "ru", "pl");
        mvc.perform(post("/language").secure(true).with(csrf()).param("language", "de"))
                .andExpect(
                        header().string(
                                        "Set-Cookie",
                                        org.hamcrest.Matchers.containsString("Secure")));
    }

    @Test
    void invalidLanguagesCannotChangeTheCookieAndTheLanguagePostRequiresCsrf() throws Exception {
        mvc.perform(post("/language").param("language", "de")).andExpect(status().isForbidden());
        mvc.perform(post("/language").with(csrf()).param("language", "es"))
                .andExpect(status().isBadRequest())
                .andExpect(header().doesNotExist("Set-Cookie"));
        mvc.perform(
                        get("/").cookie(new Cookie(SiteLocaleResolver.COOKIE_NAME, "unsupported"))
                                .header("Accept-Language", "pl-PL"))
                .andExpect(status().isOk())
                .andExpect(model().attribute("currentLanguageCode", "pl"));
        mvc.perform(
                        get("/").cookie(new Cookie(SiteLocaleResolver.COOKIE_NAME, "de"))
                                .header("Accept-Language", "tr-TR"))
                .andExpect(model().attribute("currentLanguageCode", "de"));
    }

    @Test
    void legacyLanguageUrlsMigrateWithoutDiscardingOtherQueryParameters() throws Exception {
        mvc.perform(get(URI.create("/download?art=populated&lang=de&q=A%20B&lang=fr")))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/download?art=populated&q=A%20B"))
                .andExpect(
                        header().string(
                                        "Set-Cookie",
                                        org.hamcrest.Matchers.containsString("site-language=de")));
        mvc.perform(get("/?lang=unsupported"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/"))
                .andExpect(header().doesNotExist("Set-Cookie"));
    }

    @Test
    void languageReturnLocationsRemainOnThisSite() throws Exception {
        for (String destination :
                new String[] {
                    "https://example.com",
                    "//example.com",
                    "/\\example.com",
                    "/%2f%2fexample.com",
                    "/%5cexample.com",
                    "/%0d%0aLocation:evil",
                    "/language"
                }) {
            mvc.perform(
                            post("/language")
                                    .with(csrf())
                                    .param("language", "tr")
                                    .param("returnTo", destination))
                    .andExpect(status().isSeeOther())
                    .andExpect(redirectedUrl("/"));
        }
    }

    @Test
    void administrationRequiresAuthenticationAndCsrf() throws Exception {
        mvc.perform(get("/admin"))
                .andExpect(status().isFound())
                .andExpect(redirectedUrl("/admin/login"));
        mvc.perform(post("/admin/releases/save")).andExpect(status().isForbidden());
        mvc.perform(post("/admin/releases/save").with(user("reader").roles("READER")).with(csrf()))
                .andExpect(status().isForbidden());
        mvc.perform(get("/admin").with(user("admin").roles("ADMIN"))).andExpect(status().isOk());
        mvc.perform(get("/admin/releases/new").with(user("admin").roles("ADMIN")))
                .andExpect(status().isOk());
        mvc.perform(get("/actuator/env")).andExpect(status().isFound());
    }

    @Test
    void githubAssetsPointToTheActualVersionedRelease() throws Exception {
        Release r = releases.publicVersion("0.17.7");
        Asset setup = r.setup();
        assertThat(setup).isNotNull();
        mvc.perform(get(setup.downloadUrl()))
                .andExpect(status().isFound())
                .andExpect(
                        header().string(
                                        "Location",
                                        "https://github.com/Despical/GreenLauncher/releases/download/v0.17.7/GreenLauncher-Setup-0.17.7.exe"));
        mvc.perform(get("/updates/latest.yml")).andExpect(status().isFound());
        mvc.perform(get("/changelog/9.9.9")).andExpect(status().isNotFound());
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void completeLocalReleaseLifecycleProtectsDraftsAndFiles() throws Exception {
        String previousLatest = releases.latest().version();
        String id =
                releases.save(
                        null,
                        "0.0.901",
                        "Fixture",
                        "Isolated fixture",
                        "## Test\n- Safe <script>alert(1)</script>");
        assertThatThrownBy(() -> releases.publish(id)).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> releases.save(null, "0.0.901", "Duplicate", "Fixture", "Fixture"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> releases.save(null, "001.0.0", "Invalid", "Fixture", "Fixture"))
                .isInstanceOf(IllegalArgumentException.class);
        byte[] exe = new byte[2048];
        exe[0] = 'M';
        exe[1] = 'Z';
        Asset setup =
                storage.upload(
                        id,
                        new MockMultipartFile(
                                "file",
                                "GreenLauncher-Setup-0.0.901.exe",
                                "application/octet-stream",
                                exe));
        Asset portable =
                storage.upload(
                        id,
                        new MockMultipartFile(
                                "file",
                                "GreenLauncher-0.0.901.exe",
                                "application/octet-stream",
                                exe));
        storage.upload(
                id,
                new MockMultipartFile(
                        "file",
                        "GreenLauncher-Setup-0.0.901.exe.blockmap",
                        "application/octet-stream",
                        new byte[] {1, 2, 3}));
        assertThat(setup.sha256())
                .isEqualTo(
                        HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(exe)));
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        id,
                                        new MockMultipartFile(
                                                "file",
                                                "../escape.exe",
                                                "application/octet-stream",
                                                exe)))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        id,
                                        new MockMultipartFile(
                                                "file",
                                                "GreenLauncher-0.0.901.exe",
                                                "application/octet-stream",
                                                exe)))
                .isInstanceOf(IllegalArgumentException.class);
        mvc.perform(get(portable.downloadUrl())).andExpect(status().isNotFound());
        mvc.perform(get("/changelog/0.0.901")).andExpect(status().isNotFound());
        mvc.perform(get("/admin/releases/" + id)).andExpect(status().isOk());
        releases.publish(id);
        assertThat(repository.assets(id)).hasSize(6);
        mvc.perform(get("/updates/0.0.901/latest.yml"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(setup.sha512())));
        mvc.perform(get("/updates/0.0.901/portable-update.json"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString(portable.sha256())));
        mvc.perform(get("/updates/0.0.901/SHA256SUMS.txt"))
                .andExpect(status().isOk())
                .andExpect(content().string(org.hamcrest.Matchers.containsString(setup.sha256())));
        mvc.perform(get(portable.downloadUrl()))
                .andExpect(status().isOk())
                .andExpect(content().bytes(exe))
                .andExpect(header().string("X-Checksum-Sha256", portable.sha256()));
        mvc.perform(get(portable.downloadUrl()).header("Range", "bytes=0-9"))
                .andExpect(status().isPartialContent())
                .andExpect(header().string("Content-Range", "bytes 0-9/2048"));
        mvc.perform(get("/changelog/0.0.901"))
                .andExpect(status().isOk())
                .andExpect(
                        content().string(org.hamcrest.Matchers.containsString("&lt;script&gt;")));
        assertThatThrownBy(() -> storage.delete(portable.id()))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(
                        () ->
                                storage.upload(
                                        id,
                                        new MockMultipartFile(
                                                "file",
                                                "latest.yml",
                                                "text/yaml",
                                                "test".getBytes())))
                .isInstanceOf(IllegalArgumentException.class);
        assertThat(releases.latest().version()).isEqualTo(previousLatest);
        releases.unpublish(id);
        mvc.perform(get(portable.downloadUrl())).andExpect(status().isNotFound());
        for (Asset a : repository.assets(id)) {
            storage.delete(a.id());
        }
        repository.deleteDraft(id);
    }

    @Test
    void semverOrderingNeverDowngrades() {
        assertThat(Release.compareVersions("0.18.0", "0.9.9")).isPositive();
        assertThat(Release.compareVersions("1.0.0", "0.99.99")).isPositive();
        assertThat(Release.compareVersions("0.17.7", "0.17.7")).isZero();
    }
}
