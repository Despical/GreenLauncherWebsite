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

import dev.despical.greenweb.release.ReleaseMarkdown;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ReleaseMarkdownTest {
    private final ReleaseMarkdown markdown = new ReleaseMarkdown();

    @Test
    void rendersFormattedReleaseNotesAndPlainSummaries() {
        String html = markdown.html("## Profiles\n\n- **Create profile** with *custom* `versions`.\n\n"
                + "1. Install\n2. Play\n\n> Keep worlds\n\n[Website](https://greenlauncher.org)\n\n```text\ncode\n```");
        assertThat(html).contains("<h2>Profiles</h2>", "<strong>Create profile</strong>",
                "<em>custom</em>", "<code>versions</code>", "<ol>", "<blockquote>",
                "href=\"https://greenlauncher.org\"", "<pre><code class=\"language-text\">");
        assertThat(markdown.plain("## Profiles and **versions**")).isEqualTo("Profiles and versions");
    }

    @Test
    void keepsHtmlAndUnsafeUrlsInert() {
        String html = markdown.html("<script>alert(1)</script>\n\n"
                + "[click](javascript:alert%281%29) ![image](javascript:alert%281%29)\n\n"
                + "<img src=x onerror=alert(1)>");
        assertThat(html).contains("&lt;script&gt;", "&lt;img")
                .doesNotContain("<script", "<img src=x", "href=\"javascript:", "src=\"javascript:");
    }
}
