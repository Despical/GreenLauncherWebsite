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
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.util.Base64;
import java.util.HexFormat;
import java.util.Locale;
import java.util.UUID;

/**
 * Stores immutable release files using generated storage keys rather than user-provided paths.
 * Uploads are size-limited, hashed while streaming and removed if storage or metadata persistence
 * fails.
 *
 * @author Despical
 */
@Service
public class AssetStorage {

    private final Path root;

    private final ReleaseRepository repository;

    private final ReleaseService releases;

    public AssetStorage(
            @Value("${app.storage}") String storage,
            ReleaseRepository repository,
            ReleaseService releases)
            throws IOException {
        root = Path.of(storage).toAbsolutePath().normalize().resolve("assets");
        Files.createDirectories(root);
        this.repository = repository;
        this.releases = releases;
    }

    public Asset upload(String releaseId, MultipartFile upload) throws Exception {
        String name = upload.getOriginalFilename();
        if (name != null
                && (name.equals("latest.yml")
                        || name.equals("portable-update.json")
                        || name.equals("SHA256SUMS.txt"))) {
            throw new IllegalArgumentException(
                    "Güncelleme bildirimleri ve SHA256SUMS.txt, yayın sırasında doğrulanmış"
                            + " dosyalardan otomatik oluşturulur.");
        }
        return store(releaseId, name, upload.getInputStream());
    }

    public Asset generated(String releaseId, String name, byte[] content) throws Exception {
        for (Asset old : repository.assets(releaseId)) {
            if (old.name().equals(name)) {
                delete(old.id());
            }
        }
        return store(releaseId, name, new java.io.ByteArrayInputStream(content));
    }

    private Asset store(String releaseId, String name, java.io.InputStream input) throws Exception {
        try (input) {
            Release release = releases.require(releaseId);
            if (release.published()) {
                throw new IllegalArgumentException(
                        "Yayınlanan dosyalar değiştirilemez. Önce sürümü taslağa alın.");
            }
            if (name == null
                    || !name.matches("[A-Za-z0-9][A-Za-z0-9._-]{0,159}")
                    || !allowed(name, release.version())) {
                throw new IllegalArgumentException(
                        "Dosya adı sürümle eşleşmeli: GreenLauncher-Setup-"
                                + release.version()
                                + ".exe, GreenLauncher-"
                                + release.version()
                                + ".exe veya ilgili güncelleme dosyası.");
            }
            if (repository.assets(releaseId).stream().anyMatch(a -> a.name().equals(name))) {
                throw new IllegalArgumentException("Bu dosya zaten yüklü; üzerine yazılmaz.");
            }
            String id = UUID.randomUUID().toString();
            Path target = root.resolve(id),
                    temporary = Files.createTempFile(root, "upload-", ".tmp");
            try {
                MessageDigest sha256 = MessageDigest.getInstance("SHA-256"),
                        sha512 = MessageDigest.getInstance("SHA-512");
                long size = 0;
                int first = -1, second = -1;
                try (var in = input;
                        var out = Files.newOutputStream(temporary)) {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        if (size == 0 && read >= 2) {
                            first = buffer[0] & 255;
                            second = buffer[1] & 255;
                        }
                        size += read;
                        if (size > 1073741824L) {
                            throw new IllegalArgumentException("Dosya 1 GB sınırını aşıyor.");
                        }
                        sha256.update(buffer, 0, read);
                        sha512.update(buffer, 0, read);
                        out.write(buffer, 0, read);
                    }
                }
                if (size == 0) {
                    throw new IllegalArgumentException("Dosya boş olamaz.");
                }
                if (name.endsWith(".exe") && (first != 'M' || second != 'Z')) {
                    throw new IllegalArgumentException("Geçerli bir Windows EXE dosyası gerekli.");
                }
                Files.move(temporary, target, StandardCopyOption.ATOMIC_MOVE);
                Asset asset =
                        new Asset(
                                id,
                                releaseId,
                                name,
                                kind(name),
                                size,
                                HexFormat.of().formatHex(sha256.digest()),
                                Base64.getEncoder().encodeToString(sha512.digest()),
                                null,
                                id);
                repository.insertAsset(asset);
                return asset;
            } catch (Exception failure) {
                Files.deleteIfExists(temporary);
                Files.deleteIfExists(target);
                throw failure;
            }
        }
    }

    public Path file(Asset asset) {
        if (asset.storageKey() == null || !asset.storageKey().matches("[a-f0-9-]{36}")) {
            throw new IllegalArgumentException("Geçersiz dosya.");
        }
        Path path = root.resolve(asset.storageKey()).normalize();
        if (!path.startsWith(root)) {
            throw new IllegalArgumentException("Geçersiz dosya.");
        }
        return path;
    }

    public void delete(String id) throws IOException {
        Asset asset =
                repository
                        .asset(id)
                        .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı."));
        if (releases.require(asset.releaseId()).published()) {
            throw new IllegalArgumentException("Yayınlanmış sürümün dosyaları silinemez.");
        }
        if (!asset.isLocal()) {
            throw new IllegalArgumentException("GitHub dosyaları buradan silinemez.");
        }
        repository.deleteAsset(id);
        Files.deleteIfExists(file(asset));
    }

    private static boolean allowed(String name, String v) {
        return name.equals("GreenLauncher-Setup-" + v + ".exe")
                || name.equals("GreenLauncher-Setup-" + v + ".exe.blockmap")
                || name.equals("GreenLauncher-" + v + ".exe")
                || name.equals("GreenLauncher-" + v + ".exe.blocks.json")
                || name.equals("latest.yml")
                || name.equals("portable-update.json")
                || name.equals("SHA256SUMS.txt");
    }

    public static String kind(String name) {
        String lower = name.toLowerCase(Locale.ROOT);
        if (lower.endsWith(".exe")) {
            return lower.contains("setup") ? "setup" : "portable";
        }
        return "update";
    }
}
