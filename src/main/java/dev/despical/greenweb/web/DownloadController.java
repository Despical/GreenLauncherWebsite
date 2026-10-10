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

import dev.despical.greenweb.release.Asset;
import dev.despical.greenweb.release.AssetStorage;
import dev.despical.greenweb.release.Release;
import dev.despical.greenweb.release.ReleaseRepository;
import dev.despical.greenweb.release.ReleaseService;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.net.URI;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;

/**
 * Serves published local assets and redirects imported assets to the official GitHub repository.
 * Local resources support HTTP range requests; update manifests always follow the latest stable
 * release.
 *
 * @author Despical
 */
@RestController
public class DownloadController {

    private final ReleaseRepository repository;

    private final ReleaseService releases;

    private final AssetStorage storage;

    public DownloadController(
            ReleaseRepository repository, ReleaseService releases, AssetStorage storage) {
        this.repository = repository;
        this.releases = releases;
        this.storage = storage;
    }

    @GetMapping("/downloads/{id}/{name}")
    public ResponseEntity<?> download(@PathVariable String id, @PathVariable String name) {
        Asset asset =
                repository
                        .asset(id)
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!asset.name().equals(name) || !releases.require(asset.releaseId()).published()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return response(asset);
    }

    @GetMapping("/updates/{version}/{name}")
    public ResponseEntity<?> versionAsset(@PathVariable String version, @PathVariable String name) {
        Release release = releases.publicVersion(version);
        Asset asset =
                release.assets().stream()
                        .filter(a -> a.name().equals(name))
                        .findFirst()
                        .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return response(asset);
    }

    @GetMapping(value = "/updates/latest.yml", produces = "text/yaml")
    public ResponseEntity<?> latestYaml() {
        Release release = latest();
        Asset setup = release.setup();
        if (setup == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!setup.isLocal()) {
            return response(named(release, "latest.yml"));
        }
        if (release.assets().stream().noneMatch(a -> a.name().equals(setup.name() + ".blockmap"))) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        String url = release.version() + "/" + setup.name();
        String yaml =
                "version: "
                        + release.version()
                        + "\nfiles:\n  - url: "
                        + url
                        + "\n    sha512: "
                        + setup.sha512()
                        + "\n    size: "
                        + setup.sizeBytes()
                        + "\npath: "
                        + url
                        + "\nsha512: "
                        + setup.sha512()
                        + "\nreleaseDate: '"
                        + release.publishedAt()
                        + "'\n";
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(MediaType.parseMediaType("text/yaml"))
                .body(yaml);
    }

    @GetMapping(value = "/updates/portable-update.json", produces = "application/json")
    public ResponseEntity<?> portableMetadata() {
        Release release = latest();
        Asset portable = release.portable();
        if (portable == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        if (!portable.isLocal()) {
            return response(named(release, "portable-update.json"));
        }
        return portableResponse(release);
    }

    @GetMapping("/updates/{name}")
    public ResponseEntity<?> latestAsset(@PathVariable String name) {
        return response(named(latest(), name));
    }

    private ResponseEntity<?> portableResponse(Release release) {
        return response(named(release, "portable-update.json"));
    }

    private Release latest() {
        Release r = releases.latest();
        if (r == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return r;
    }

    private Asset named(Release release, String name) {
        return release.assets().stream()
                .filter(a -> a.name().equals(name))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    private ResponseEntity<?> response(Asset asset) {
        if (!asset.isLocal()) {
            if (!asset.externalUrl()
                    .startsWith("https://github.com/Despical/GreenLauncher/releases/download/")) {
                throw new ResponseStatusException(HttpStatus.NOT_FOUND);
            }
            return ResponseEntity.status(HttpStatus.FOUND)
                    .location(URI.create(asset.externalUrl()))
                    .cacheControl(CacheControl.noStore())
                    .build();
        }
        var file = storage.file(asset);
        if (!Files.isRegularFile(file)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        Resource resource = new FileSystemResource(file);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .contentLength(asset.sizeBytes())
                .header(
                        HttpHeaders.CONTENT_DISPOSITION,
                        ContentDisposition.attachment()
                                .filename(asset.name(), StandardCharsets.UTF_8)
                                .build()
                                .toString())
                .header("X-Checksum-Sha256", asset.sha256())
                .eTag('"' + asset.sha256() + '"')
                .body(resource);
    }
}
