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

import dev.despical.greenweb.release.ReleaseMarkdown;
import dev.despical.greenweb.release.ReleaseService;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;

import java.util.List;

/**
 * Renders public launcher pages using published releases and localized question lists.
 *
 * @author Despical
 */
@Controller
public class PublicController {

    private static final List<String> HOME_QUESTIONS =
            List.of("free", "account", "system", "mods", "profiles");

    private static final List<String> ALL_QUESTIONS =
            List.of(
                    "free",
                    "account",
                    "system",
                    "java",
                    "profiles",
                    "memory",
                    "loaders",
                    "mods",
                    "modpacks",
                    "shaders",
                    "worlds",
                    "import",
                    "update",
                    "logs",
                    "help");

    private final ReleaseService releases;

    private final ReleaseMarkdown markdown;

    public PublicController(ReleaseService releases, ReleaseMarkdown markdown) {
        this.releases = releases;
        this.markdown = markdown;
    }

    @ModelAttribute
    void shared(Model model) {
        model.addAttribute("latest", releases.latest());
    }

    @GetMapping("/")
    String home(Model model) {
        model.addAttribute("activePage", "home");
        model.addAttribute("releases", releases.publicReleases().stream().limit(3).toList());
        model.addAttribute("faqQuestions", HOME_QUESTIONS);
        return "home";
    }

    @GetMapping("/features")
    String features(Model model) {
        model.addAttribute("activePage", "features");
        return "features";
    }

    @GetMapping("/faq")
    String faq(Model model) {
        model.addAttribute("activePage", "faq");
        model.addAttribute("faqQuestions", ALL_QUESTIONS);
        return "faq";
    }

    @GetMapping("/download")
    String download(Model model) {
        model.addAttribute("activePage", "download");
        return "download";
    }

    @GetMapping("/changelog")
    String changelog(Model model) {
        model.addAttribute("activePage", "changelog");
        model.addAttribute("releases", releases.publicReleases());
        return "changelog";
    }

    @GetMapping("/changelog/{version}")
    String detail(@PathVariable String version, Model model) {
        model.addAttribute("activePage", "changelog");
        var release = releases.publicVersion(version);
        model.addAttribute("release", release);
        model.addAttribute("releaseNotesHtml", markdown.html(release.notes()));
        return "release";
    }
}
