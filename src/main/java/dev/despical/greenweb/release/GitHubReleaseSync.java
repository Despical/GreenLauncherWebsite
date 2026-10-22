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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;

/** Periodically imports stable releases without changing existing release records. */
@Configuration(proxyBeanMethods = false)
@EnableScheduling
@ConditionalOnProperty(name = "app.github-sync.enabled", havingValue = "true")
public class GitHubReleaseSync {

    private static final Logger LOGGER = LoggerFactory.getLogger(GitHubReleaseSync.class);

    private final GitHubImportService github;

    public GitHubReleaseSync(GitHubImportService github) {
        this.github = github;
    }

    @Scheduled(
            initialDelayString = "${app.github-sync.initial-delay:15s}",
            fixedDelayString = "${app.github-sync.interval:5m}")
    public void sync() {
        try {
            int count = github.sync();
            LOGGER.info("GitHub release sync completed: {} new releases", count);
        } catch (Exception error) {
            if (error instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            LOGGER.warn("GitHub release sync failed; existing releases preserved. Will retry next interval.", error);
        }
    }
}
