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

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * Immutable release metadata used by public pages and the administrator editor. Asset selection and
 * date formatting are presentation helpers; version ordering is numeric.
 *
 * @author Despical
 */
public record Release(
        String id,
        String version,
        String title,
        String summary,
        String notes,
        boolean published,
        String publishedAt,
        String createdAt,
        String sourceUrl,
        List<Asset> assets) {

    public Asset setup() {
        return assets.stream().filter(a -> a.kind().equals("setup")).findFirst().orElse(null);
    }

    public Asset portable() {
        return assets.stream().filter(a -> a.kind().equals("portable")).findFirst().orElse(null);
    }

    public String dateLabel() {
        return DateTimeFormatter.ofPattern("dd.MM.yyyy")
                .withZone(ZoneId.of("Europe/Istanbul"))
                .format(Instant.parse(publishedAt == null ? createdAt : publishedAt));
    }

    public static int compareVersions(String left, String right) {
        String[] a = left.split("\\."), b = right.split("\\.");
        for (int i = 0; i < 3; i++) {
            int n = Integer.compare(Integer.parseInt(a[i]), Integer.parseInt(b[i]));
            if (n != 0) {
                return n;
            }
        }
        return 0;
    }
}
