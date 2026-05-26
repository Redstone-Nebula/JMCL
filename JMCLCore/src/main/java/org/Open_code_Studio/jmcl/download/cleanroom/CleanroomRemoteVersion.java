/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2021  huangyuhui <huanghongxun2008@126.com> and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */
package org.Open_code_Studio.jmcl.download.cleanroom;

import org.Open_code_Studio.jmcl.download.DefaultDependencyManager;
import org.Open_code_Studio.jmcl.download.LibraryAnalyzer;
import org.Open_code_Studio.jmcl.download.RemoteVersion;
import org.Open_code_Studio.jmcl.game.Version;
import org.Open_code_Studio.jmcl.task.Task;

import java.time.Instant;
import java.util.List;

public class CleanroomRemoteVersion extends RemoteVersion {
    public CleanroomRemoteVersion(String gameVersion, String selfVersion, Instant releaseDate, List<String> url) {
        super(LibraryAnalyzer.LibraryType.CLEANROOM.getPatchId(), gameVersion, selfVersion, releaseDate, url);
    }

    @Override
    public Task<Version> getInstallTask(DefaultDependencyManager dependencyManager, Version baseVersion) {
        return new CleanroomInstallTask(dependencyManager, baseVersion, this);
    }
}
