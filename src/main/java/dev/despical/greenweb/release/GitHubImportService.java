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

package dev.despical.greenweb.release;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.json.JsonMapper;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Imports new stable releases from the official launcher repository. Existing release metadata is
 * preserved; the bundled public snapshot seeds an empty installation.
 *
 * @author Despical
 */
@Service
public class GitHubImportService {

    private final ReleaseRepository repository;

    private final TransactionTemplate transaction;

    private final JsonMapper mapper = JsonMapper.builder().build();

    public GitHubImportService(ReleaseRepository repository, PlatformTransactionManager transactions) {
        this.repository = repository;
        this.transaction = new TransactionTemplate(transactions);
    }

    @Bean
    ApplicationRunner seed(@Value("${app.seed-releases:true}") boolean enabled) {
        return args -> {
            if (enabled && repository.all(false).isEmpty()) {
                try (var in =
                        new ClassPathResource("releases/github-releases.json").getInputStream()) {
                    importJson(mapper.readTree(in));
                }
            }
        };
    }

    public int sync() throws Exception {
        try (HttpClient client =
                HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build()) {
            var response =
                    client.send(
                            HttpRequest.newBuilder(
                                            URI.create(
                                                    "https://api.github.com/repos/Despical/GreenLauncher/releases?per_page=100"))
                                    .timeout(Duration.ofSeconds(25))
                                    .header("Accept", "application/vnd.github+json")
                                    .header("User-Agent", "GreenLauncherWeb/1.0")
                                    .GET()
                                    .build(),
                            HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != 200) {
                throw new IllegalArgumentException(
                        "GitHub'a ulaşılamadı ("
                                + response.statusCode()
                                + "). Mevcut sürümler korunuyor.");
            }
            return importJson(mapper.readTree(response.body()));
        }
    }

    public synchronized int importJson(JsonNode releases) {
        return transaction.execute(status -> importReleases(releases));
    }

    private int importReleases(JsonNode releases) {
        int count = 0;
        for (JsonNode item : releases) {
            if (item.path("draft").asBoolean() || item.path("prerelease").asBoolean()) {
                continue;
            }
            String version = item.path("tag_name").asString().replaceFirst("^v", "");
            if (!version.matches("(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})")
                    || repository.byVersion(version).isPresent()) {
                continue;
            }
            String id = UUID.randomUUID().toString(), notes = item.path("body").asString("");
            String summary = notes.split("\\n", 2)[0];
            String source = item.path("html_url").asString();
            if (!source.startsWith("https://github.com/Despical/GreenLauncher/releases/tag/")) {
                continue;
            }
            repository.insert(
                    new Release(
                            id,
                            version,
                            "Green Launcher " + version,
                            summary.substring(0, Math.min(800, summary.length())),
                            notes.substring(0, Math.min(40000, notes.length())),
                            true,
                            item.path("published_at").asString(Instant.now().toString()),
                            Instant.now().toString(),
                            source,
                            List.of()));
            for (JsonNode asset : item.path("assets")) {
                String name = asset.path("name").asString(),
                        url = asset.path("browser_download_url").asString();
                if (!name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,159}")
                        || !url.startsWith(
                                "https://github.com/Despical/GreenLauncher/releases/download/")) {
                    continue;
                }
                String digest = asset.path("digest").asString("");
                repository.insertAsset(
                        new Asset(
                                UUID.randomUUID().toString(),
                                id,
                                name,
                                AssetStorage.kind(name),
                                asset.path("size").asLong(),
                                digest.startsWith("sha256:") ? digest.substring(7) : null,
                                null,
                                url,
                                null));
            }
            count++;
        }
        return count;
    }
}
