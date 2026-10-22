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

import dev.despical.greenweb.release.GitHubImportService;
import dev.despical.greenweb.release.ReleaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import tools.jackson.databind.json.JsonMapper;

import static org.assertj.core.api.Assertions.*;

@SpringBootTest(properties = {
        "spring.datasource.url=jdbc:h2:mem:github-import;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
        "app.seed-releases=false", "app.local-admin=false",
        "app.admin-password-hash=$2a$10$N9qo8uLOickgx2ZMRZoMyeIjZAgcfl7p92ldGxad68LJZdL17lhWy",
})
class GitHubImportIntegrationTest {
    @Autowired GitHubImportService github;
    @Autowired ReleaseRepository repository;
    private final JsonMapper mapper = JsonMapper.builder().build();

    private String release(String version, String assets) {
        return """
                {"tag_name":"v%s","body":"## Profiles\\n\\n- **Create profile**",
                 "html_url":"https://github.com/Despical/GreenLauncher/releases/tag/v%s",
                 "published_at":"2026-10-22T12:02:27Z","assets":[%s]}
                """.formatted(version, version, assets);
    }

    private String asset(String version) {
        return """
                {"name":"GreenLauncher-%s.exe","size":2048,
                 "browser_download_url":"https://github.com/Despical/GreenLauncher/releases/download/v%s/GreenLauncher-%s.exe"}
                """.formatted(version, version, version);
    }

    @Test
    void importsStableReleasesOnceAndPreservesExistingEditsAndDrafts() throws Exception {
        var fixture = mapper.readTree("[" + release("0.20.0", asset("0.20.0")) + ","
                + release("0.21.0", "").replace("{\"tag_name\"", "{\"draft\":true,\"tag_name\"") + ","
                + release("0.22.0", "").replace("{\"tag_name\"", "{\"prerelease\":true,\"tag_name\"") + "]");
        assertThat(github.importJson(fixture)).isEqualTo(1);
        var imported = repository.byVersion("0.20.0").orElseThrow();
        assertThat(imported.published()).isTrue();
        assertThat(imported.assets()).hasSize(1);
        repository.edit(imported.id(), "Custom title", "Custom summary", "Custom notes");
        repository.publish(imported.id(), false, imported.publishedAt());
        assertThat(github.importJson(fixture)).isZero();
        var preserved = repository.byVersion("0.20.0").orElseThrow();
        assertThat(preserved.title()).isEqualTo("Custom title");
        assertThat(preserved.notes()).isEqualTo("Custom notes");
        assertThat(preserved.published()).isFalse();
        assertThat(repository.byVersion("0.21.0")).isEmpty();
        assertThat(repository.byVersion("0.22.0")).isEmpty();
    }

    @Test
    void failedAssetImportRollsBackReleaseAndAllowsRetry() throws Exception {
        var invalid = mapper.readTree("[" + release("0.23.0", asset("0.23.0") + "," + asset("0.23.0")) + "]");
        assertThatThrownBy(() -> github.importJson(invalid)).isInstanceOf(RuntimeException.class);
        assertThat(repository.byVersion("0.23.0")).isEmpty();
        assertThat(github.importJson(mapper.readTree("[" + release("0.23.0", asset("0.23.0")) + "]"))).isEqualTo(1);
    }
}
