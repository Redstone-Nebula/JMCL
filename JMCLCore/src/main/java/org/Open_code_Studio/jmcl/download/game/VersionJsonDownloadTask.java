/*
 * JMCL
 * Copyright (C) 2026  OCS
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
package org.Open_code_Studio.jmcl.download.game;

import org.Open_code_Studio.jmcl.download.DefaultDependencyManager;
import org.Open_code_Studio.jmcl.download.RemoteVersion;
import org.Open_code_Studio.jmcl.download.VersionList;
import org.Open_code_Studio.jmcl.task.GetTask;
import org.Open_code_Studio.jmcl.task.Task;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 *
 * @author huangyuhui
 */
public final class VersionJsonDownloadTask extends Task<String> {
    private final String gameVersion;
    private final DefaultDependencyManager dependencyManager;
    private final List<Task<?>> dependents = new ArrayList<>(1);
    private final List<Task<?>> dependencies = new ArrayList<>(1);
    private final VersionList<?> gameVersionList;

    public VersionJsonDownloadTask(String gameVersion, DefaultDependencyManager dependencyManager) {
        this.gameVersion = gameVersion;
        this.dependencyManager = dependencyManager;
        this.gameVersionList = dependencyManager.getVersionList("game");

        dependents.add(gameVersionList.loadAsync(gameVersion));

        setSignificance(TaskSignificance.MODERATE);
    }

    @Override
    public Collection<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public Collection<Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public void execute() throws IOException {
        RemoteVersion remoteVersion = gameVersionList.getVersion(gameVersion, gameVersion)
                .orElseThrow(() -> new IOException("Cannot find specific version " + gameVersion + " in remote repository"));
        dependencies.add(new GetTask(dependencyManager.getDownloadProvider().injectURLsWithCandidates(remoteVersion.getUrls())).storeTo(this::setResult));
    }
}
