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
import dev.despical.greenweb.release.GitHubImportService;
import dev.despical.greenweb.release.Release;
import dev.despical.greenweb.release.ReleaseRepository;
import dev.despical.greenweb.release.ReleaseService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.Map;

/**
 * Handles authenticated release editing, uploads, publication and GitHub synchronization. Every
 * mutation uses POST with CSRF protection supplied by Spring Security.
 *
 * @author Despical
 */
@Controller
@RequestMapping("/admin")
public class AdminController {

    private final ReleaseService releases;

    private final ReleaseRepository repository;

    private final AssetStorage storage;

    private final GitHubImportService github;

    public AdminController(
            ReleaseService releases,
            ReleaseRepository repository,
            AssetStorage storage,
            GitHubImportService github) {
        this.releases = releases;
        this.repository = repository;
        this.storage = storage;
        this.github = github;
    }

    @GetMapping("/login")
    String login() {
        return "admin/login";
    }

    @GetMapping
    String dashboard(Model model) {
        model.addAttribute("releases", repository.all(false));
        model.addAttribute("publishedCount", releases.publicReleases().size());
        return "admin/dashboard";
    }

    @GetMapping("/releases/new")
    String create() {
        return "admin/editor";
    }

    @GetMapping("/releases/{id}")
    String editor(@PathVariable String id, Model model) {
        model.addAttribute("release", releases.require(id));
        return "admin/editor";
    }

    @PostMapping("/releases/save")
    String save(@RequestParam Map<String, String> form, RedirectAttributes flash) {
        String id = form.get("id");
        try {
            id =
                    releases.save(
                            id,
                            form.get("version"),
                            form.get("title"),
                            form.get("summary"),
                            form.get("notes"));
            flash.addFlashAttribute("success", "Sürüm kaydedildi.");
            return "redirect:/admin/releases/" + id;
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage());
            flash.addFlashAttribute("form", form);
            return "redirect:/admin/releases/" + (id == null || id.isBlank() ? "new" : id);
        }
    }

    @PostMapping("/releases/{id}/publish")
    String publish(@PathVariable String id, RedirectAttributes flash) {
        try {
            releases.publish(id);
            flash.addFlashAttribute(
                    "success", "Sürüm yayınlandı. İndirmeler ve sürüm notları artık herkese açık.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/releases/" + id;
    }

    @PostMapping("/releases/{id}/unpublish")
    String unpublish(@PathVariable String id, RedirectAttributes flash) {
        releases.unpublish(id);
        flash.addFlashAttribute("success", "Sürüm taslağa alındı.");
        return "redirect:/admin/releases/" + id;
    }

    @PostMapping("/releases/{id}/upload")
    String upload(
            @PathVariable String id,
            @RequestParam("file") MultipartFile file,
            RedirectAttributes flash) {
        releases.require(id);
        try {
            storage.upload(id, file);
            flash.addFlashAttribute(
                    "success",
                    "Dosya yüklendi ve SHA-256 / SHA-512 doğrulama değerleri hesaplandı.");
        } catch (Exception error) {
            flash.addFlashAttribute(
                    "error",
                    error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "Dosya kaydedilemedi. Depolama alanını kontrol edin.");
        }
        return "redirect:/admin/releases/" + id;
    }

    @PostMapping("/assets/{id}/delete")
    String deleteAsset(@PathVariable String id, RedirectAttributes flash) throws Exception {
        Asset a =
                repository
                        .asset(id)
                        .orElseThrow(() -> new IllegalArgumentException("Dosya bulunamadı."));
        try {
            storage.delete(id);
            flash.addFlashAttribute("success", "Dosya silindi.");
        } catch (IllegalArgumentException error) {
            flash.addFlashAttribute("error", error.getMessage());
        }
        return "redirect:/admin/releases/" + a.releaseId();
    }

    @PostMapping("/releases/{id}/delete")
    String delete(@PathVariable String id, RedirectAttributes flash) throws Exception {
        Release r = releases.require(id);
        if (r.published()) {
            flash.addFlashAttribute("error", "Yayınlanmış sürüm silinemez. Önce taslağa alın.");
            return "redirect:/admin/releases/" + id;
        }
        for (Asset a : r.assets()) {
            if (a.isLocal()) {
                storage.delete(a.id());
            }
        }
        repository.deleteDraft(id);
        flash.addFlashAttribute("success", "Taslak silindi.");
        return "redirect:/admin";
    }

    @PostMapping("/sync")
    String sync(RedirectAttributes flash) {
        try {
            int count = github.sync();
            flash.addFlashAttribute("success", count + " yeni GitHub sürümü içe aktarıldı.");
        } catch (Exception error) {
            flash.addFlashAttribute(
                    "error",
                    error instanceof IllegalArgumentException
                            ? error.getMessage()
                            : "GitHub'a ulaşılamadı. Mevcut sürümler korunuyor.");
        }
        return "redirect:/admin";
    }
}
