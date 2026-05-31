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
package org.Open_code_Studio.jmcl.game;

import com.google.gson.JsonParseException;
import org.Open_code_Studio.jmcl.download.DefaultDependencyManager;
import org.Open_code_Studio.jmcl.download.LibraryAnalyzer;
import org.Open_code_Studio.jmcl.mod.MinecraftInstanceTask;
import org.Open_code_Studio.jmcl.mod.Modpack;
import org.Open_code_Studio.jmcl.mod.ModpackConfiguration;
import org.Open_code_Studio.jmcl.mod.ModpackInstallTask;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class JMCLModpackInstallTask extends Task<Void> {
    private final Path zipFile;
    private final String name;
    private final JMCLGameRepository repository;
    private final DefaultDependencyManager dependency;
    private final Modpack modpack;
    private final List<Task<?>> dependencies = new ArrayList<>(1);
    private final List<Task<?>> dependents = new ArrayList<>(4);

    public JMCLModpackInstallTask(Profile profile, Path zipFile, Modpack modpack, String name) {
        dependency = profile.getDependency();
        repository = profile.getRepository();
        this.zipFile = zipFile;
        this.name = name;
        this.modpack = modpack;

        Path run = repository.getRunDirectory(name);
        Path json = repository.getModpackConfiguration(name);
        if (repository.hasVersion(name) && Files.notExists(json))
            throw new IllegalArgumentException("Version " + name + " already exists");

        dependents.add(dependency.gameBuilder().name(name).gameVersion(modpack.getGameVersion()).buildAsync());

        onDone().register(event -> {
            if (event.isFailed()) repository.removeVersionFromDisk(name);
        });

        ModpackConfiguration<Modpack> config = null;
        try {
            if (Files.exists(json)) {
                config = JsonUtils.fromJsonFile(json, ModpackConfiguration.typeOf(Modpack.class));

                if (!JMCLModpackProvider.INSTANCE.getName().equals(config.getType()))
                    throw new IllegalArgumentException("Version " + name + " is not a HMCL modpack. Cannot update this version.");
            }
        } catch (JsonParseException | IOException ignore) {
        }
        dependents.add(new ModpackInstallTask<>(zipFile, run, modpack.getEncoding(), Collections.singletonList("/minecraft"), it -> !"pack.json".equals(it), config));
        dependents.add(new MinecraftInstanceTask<>(zipFile, modpack.getEncoding(), Collections.singletonList("/minecraft"), modpack, JMCLModpackProvider.INSTANCE, modpack.getName(), modpack.getVersion(), repository.getModpackConfiguration(name)).withStage("jvmmcl.modpack"));
    }

    @Override
    public List<Task<?>> getDependencies() {
        return dependencies;
    }

    @Override
    public List<Task<?>> getDependents() {
        return dependents;
    }

    @Override
    public void execute() throws Exception {
        String json = CompressingUtils.readTextZipEntry(zipFile, "minecraft/pack.json");
        Version originalVersion = JsonUtils.GSON.fromJson(json, Version.class).setId(name).setJar(null);
        LibraryAnalyzer analyzer = LibraryAnalyzer.analyze(originalVersion, null);
        Task<Version> libraryTask = Task.supplyAsync(() -> originalVersion);
        // reinstall libraries
        // libraries of Forge and OptiFine should be obtained by installation.
        for (LibraryAnalyzer.LibraryMark mark : analyzer) {
            if (LibraryAnalyzer.LibraryType.MINECRAFT.getPatchId().equals(mark.getLibraryId()))
                continue;
            libraryTask = libraryTask.thenComposeAsync(version -> dependency.installLibraryAsync(modpack.getGameVersion(), version, mark.getLibraryId(), mark.getLibraryVersion()));
        }

        dependencies.add(libraryTask.thenComposeAsync(repository::saveAsync));
    }
}
