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

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import tools.jackson.databind.json.JsonMapper;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Coordinates draft editing, publication and generation of verified update manifests. Published
 * files are immutable, and the current release is determined by semantic version ordering.
 *
 * @author Despical
 */
@Service
public class ReleaseService {

    private final ReleaseRepository repository;

    private final ObjectProvider<AssetStorage> storage;

    public ReleaseService(ReleaseRepository repository, ObjectProvider<AssetStorage> storage) {
        this.repository = repository;
        this.storage = storage;
    }

    public List<Release> publicReleases() {
        return repository.all(true);
    }

    public Release latest() {
        return publicReleases().stream().findFirst().orElse(null);
    }

    public Release require(String id) {
        return repository
                .find(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Release publicVersion(String version) {
        return repository
                .byVersion(version)
                .filter(Release::published)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public String save(String id, String version, String title, String summary, String notes) {
        title = checked(title, 160, "Başlık");
        summary = checked(summary, 800, "Özet");
        notes = checked(notes, 40000, "Sürüm notları");
        if (id == null || id.isBlank()) {
            if (version == null
                    || !version.matches(
                            "(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})\\.(0|[1-9][0-9]{0,5})")) {
                throw new IllegalArgumentException("Sürüm 0.18.0 gibi üç sayıyla yazılmalı.");
            }
            if (repository.byVersion(version).isPresent()) {
                throw new IllegalArgumentException("Bu sürüm zaten mevcut.");
            }
            id = UUID.randomUUID().toString();
            repository.insert(
                    new Release(
                            id,
                            version,
                            title,
                            summary,
                            notes,
                            false,
                            null,
                            Instant.now().toString(),
                            null,
                            List.of()));
        } else {
            require(id);
            repository.edit(id, title, summary, notes);
        }
        return id;
    }

    @Transactional
    public void publish(String id) {
        Release r = require(id);
        if (r.setup() == null || r.portable() == null) {
            throw new IllegalArgumentException(
                    "Yayınlamak için setup ve portable EXE dosyaları gerekli.");
        }
        if (r.published()) {
            return;
        }
        String now = Instant.now().toString();
        if (r.setup().isLocal() && r.portable().isLocal()) {
            if (r.assets().stream()
                    .noneMatch(a -> a.name().equals(r.setup().name() + ".blockmap"))) {
                throw new IllegalArgumentException(
                        "Setup güncellemeleri için eşleşen .exe.blockmap dosyasını da ekle.");
            }
            try {
                generateManifests(r, now);
            } catch (Exception error) {
                throw new IllegalArgumentException(
                        "Güncelleme dosyaları oluşturulamadı; sürüm yayınlanmadı.", error);
            }
        }
        repository.publish(id, true, now);
    }

    private void generateManifests(Release r, String timestamp) throws Exception {
        Asset setup = r.setup(), portable = r.portable();
        AssetStorage files = storage.getObject();
        String yaml =
                "version: "
                        + r.version()
                        + "\nfiles:\n  - url: "
                        + setup.name()
                        + "\n    sha512: "
                        + setup.sha512()
                        + "\n    size: "
                        + setup.sizeBytes()
                        + "\npath: "
                        + setup.name()
                        + "\nsha512: "
                        + setup.sha512()
                        + "\nreleaseDate: '"
                        + timestamp
                        + "'\n";
        files.generated(r.id(), "latest.yml", yaml.getBytes(StandardCharsets.UTF_8));
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("version", r.version());
        metadata.put("file", portable.name());
        metadata.put("sha256", portable.sha256());
        metadata.put("size", portable.sizeBytes());
        r.assets().stream()
                .filter(a -> a.name().equals(portable.name() + ".blocks.json"))
                .findFirst()
                .ifPresent(
                        blocks ->
                                metadata.put(
                                        "blockmap",
                                        Map.of(
                                                "file",
                                                blocks.name(),
                                                "sha256",
                                                blocks.sha256(),
                                                "size",
                                                blocks.sizeBytes())));
        files.generated(
                r.id(),
                "portable-update.json",
                JsonMapper.builder().build().writeValueAsBytes(metadata));
        String sums =
                repository.assets(r.id()).stream()
                        .filter(a -> !a.name().equals("SHA256SUMS.txt"))
                        .map(a -> a.sha256() + "  " + a.name() + "\n")
                        .collect(java.util.stream.Collectors.joining());
        files.generated(r.id(), "SHA256SUMS.txt", sums.getBytes(StandardCharsets.UTF_8));
    }

    public void unpublish(String id) {
        Release r = require(id);
        repository.publish(id, false, r.publishedAt());
    }

    private String checked(String value, int max, String field) {
        if (value == null || value.isBlank() || value.length() > max) {
            throw new IllegalArgumentException(field + " gerekli; en fazla " + max + " karakter.");
        }
        return value.strip();
    }
}
