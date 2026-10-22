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

import dev.despical.greenweb.release.GitHubImportService;
import dev.despical.greenweb.release.GitHubReleaseSync;
import org.junit.jupiter.api.Test;

import static org.mockito.Mockito.*;

class GitHubReleaseSyncTest {
    @Test
    void aFailedCheckDoesNotPreventTheNextCheck() throws Exception {
        GitHubImportService github = mock(GitHubImportService.class);
        when(github.sync()).thenThrow(new IllegalStateException("GitHub unavailable")).thenReturn(1);
        GitHubReleaseSync sync = new GitHubReleaseSync(github);
        sync.sync();
        sync.sync();
        verify(github, times(2)).sync();
    }
}
