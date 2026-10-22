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

import org.commonmark.parser.Parser;
import org.commonmark.renderer.html.HtmlRenderer;
import org.commonmark.renderer.text.TextContentRenderer;
import org.springframework.stereotype.Component;

/** Renders release Markdown while keeping embedded HTML and unsafe URLs inert. */
@Component
public class ReleaseMarkdown {

    private final Parser parser = Parser.builder().build();
    private final HtmlRenderer html =
            HtmlRenderer.builder().escapeHtml(true).sanitizeUrls(true).build();
    private final TextContentRenderer text = TextContentRenderer.builder().build();

    public String html(String source) {
        return html.render(parser.parse(source == null ? "" : source));
    }

    public String plain(String source) {
        return text.render(parser.parse(source == null ? "" : source)).strip();
    }
}
