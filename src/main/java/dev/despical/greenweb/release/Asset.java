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

/**
 * Immutable metadata for a locally stored or trusted external release asset. Checksums describe the
 * bytes captured during upload or provided by GitHub.
 *
 * @author Despical
 */
public record Asset(
        String id,
        String releaseId,
        String name,
        String kind,
        long sizeBytes,
        String sha256,
        String sha512,
        String externalUrl,
        String storageKey) {

    public String sizeLabel() {
        return sizeBytes >= 1048576
                ? "%.1f MB".formatted(sizeBytes / 1048576d)
                : "%.1f KB".formatted(sizeBytes / 1024d);
    }

    public String downloadUrl() {
        return "/downloads/" + id + "/" + name;
    }

    public boolean isLocal() {
        return storageKey != null;
    }
}
