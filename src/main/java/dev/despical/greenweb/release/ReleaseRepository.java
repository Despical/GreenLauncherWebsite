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

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * Stores release metadata and asset references through parameterized JDBC queries. Public callers
 * must additionally enforce publication status before serving assets.
 *
 * @author Despical
 */
@Repository
public class ReleaseRepository {

    private final JdbcTemplate jdbc;

    public ReleaseRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Release> all(boolean onlyPublished) {
        return jdbc
                .query(
                        "SELECT * FROM releases" + (onlyPublished ? " WHERE published = TRUE" : ""),
                        (rs, n) ->
                                new Release(
                                        rs.getString("id"),
                                        rs.getString("version"),
                                        rs.getString("title"),
                                        rs.getString("summary"),
                                        rs.getString("notes"),
                                        rs.getBoolean("published"),
                                        rs.getString("published_at"),
                                        rs.getString("created_at"),
                                        rs.getString("source_url"),
                                        assets(rs.getString("id"))))
                .stream()
                .sorted((a, b) -> Release.compareVersions(b.version(), a.version()))
                .toList();
    }

    public Optional<Release> find(String id) {
        return all(false).stream().filter(r -> r.id().equals(id)).findFirst();
    }

    public Optional<Release> byVersion(String version) {
        return all(false).stream().filter(r -> r.version().equals(version)).findFirst();
    }

    public List<Asset> assets(String releaseId) {
        return jdbc.query(
                "SELECT * FROM assets WHERE release_id = ? ORDER BY kind, name",
                (rs, n) ->
                        new Asset(
                                rs.getString("id"),
                                rs.getString("release_id"),
                                rs.getString("name"),
                                rs.getString("kind"),
                                rs.getLong("size_bytes"),
                                rs.getString("sha256"),
                                rs.getString("sha512"),
                                rs.getString("external_url"),
                                rs.getString("storage_key")),
                releaseId);
    }

    public Optional<Asset> asset(String id) {
        return jdbc
                .query(
                        "SELECT * FROM assets WHERE id = ?",
                        (rs, n) ->
                                new Asset(
                                        rs.getString("id"),
                                        rs.getString("release_id"),
                                        rs.getString("name"),
                                        rs.getString("kind"),
                                        rs.getLong("size_bytes"),
                                        rs.getString("sha256"),
                                        rs.getString("sha512"),
                                        rs.getString("external_url"),
                                        rs.getString("storage_key")),
                        id)
                .stream()
                .findFirst();
    }

    public void insert(Release r) {
        jdbc.update(
                "INSERT INTO"
                    + " releases(id,version,title,summary,notes,published,published_at,created_at,source_url)"
                    + " VALUES (?,?,?,?,?,?,?,?,?)",
                r.id(),
                r.version(),
                r.title(),
                r.summary(),
                r.notes(),
                r.published(),
                r.publishedAt(),
                r.createdAt(),
                r.sourceUrl());
    }

    public void edit(String id, String title, String summary, String notes) {
        jdbc.update(
                "UPDATE releases SET title=?, summary=?, notes=? WHERE id=?",
                title,
                summary,
                notes,
                id);
    }

    public void publish(String id, boolean published, String timestamp) {
        jdbc.update(
                "UPDATE releases SET published=?, published_at=? WHERE id=?",
                published,
                timestamp,
                id);
    }

    public void insertAsset(Asset a) {
        jdbc.update(
                "INSERT INTO"
                    + " assets(id,release_id,name,kind,size_bytes,sha256,sha512,external_url,storage_key)"
                    + " VALUES (?,?,?,?,?,?,?,?,?)",
                a.id(),
                a.releaseId(),
                a.name(),
                a.kind(),
                a.sizeBytes(),
                a.sha256(),
                a.sha512(),
                a.externalUrl(),
                a.storageKey());
    }

    public void deleteAsset(String id) {
        jdbc.update("DELETE FROM assets WHERE id=?", id);
    }

    public void deleteDraft(String id) {
        jdbc.update("DELETE FROM releases WHERE id=? AND published=FALSE", id);
    }
}
