/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2022  huangyuhui <huanghongxun2008@126.com> and contributors
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
import kala.compress.archivers.zip.ZipArchiveReader;
import org.Open_code_Studio.jmcl.download.DefaultDependencyManager;
import org.Open_code_Studio.jmcl.mod.MismatchedModpackTypeException;
import org.Open_code_Studio.jmcl.mod.Modpack;
import org.Open_code_Studio.jmcl.mod.ModpackProvider;
import org.Open_code_Studio.jmcl.mod.ModpackUpdateTask;
import org.Open_code_Studio.jmcl.setting.Profile;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.gson.JsonUtils;
import org.Open_code_Studio.jmcl.util.io.CompressingUtils;

import java.io.IOException;
import java.nio.charset.Charset;
import java.nio.file.Path;

public final class JMCLModpackProvider implements ModpackProvider {
    public static final JMCLModpackProvider INSTANCE = new JMCLModpackProvider();

    @Override
    public String getName() {
        return "JVM-MCL";
    }

    @Override
    public Task<?> createCompletionTask(DefaultDependencyManager dependencyManager, String version) {
        return null;
    }

    @Override
    public Task<?> createUpdateTask(DefaultDependencyManager dependencyManager, String name, Path zipFile, Modpack modpack) throws MismatchedModpackTypeException {
        if (!(modpack.getManifest() instanceof JMCLModpackManifest))
            throw new MismatchedModpackTypeException(getName(), modpack.getManifest().getProvider().getName());

        if (!(dependencyManager.getGameRepository() instanceof JMCLGameRepository repository)) {
            throw new IllegalArgumentException("JVM-MCLModpackProvider requires JVM-MCLGameRepository");
        }

        Profile profile = repository.getProfile();

        return new ModpackUpdateTask(dependencyManager.getGameRepository(), name, new JMCLModpackInstallTask(profile, zipFile, modpack, name));
    }

    @Override
    public Modpack readManifest(ZipArchiveReader file, Path path, Charset encoding) throws IOException, JsonParseException {
        String manifestJson = CompressingUtils.readTextZipEntry(file, "modpack.json");
        Modpack manifest = JsonUtils.fromNonNullJson(manifestJson, HMCLModpack.class).setEncoding(encoding);
        String gameJson = CompressingUtils.readTextZipEntry(file, "minecraft/pack.json");
        Version game = JsonUtils.fromNonNullJson(gameJson, Version.class);
        if (game.getJar() == null)
            if (StringUtils.isBlank(manifest.getVersion()))
                throw new JsonParseException("Cannot recognize the game version of modpack " + file + ".");
            else
                manifest.setManifest(JMCLModpackManifest.INSTANCE);
        else
            manifest.setManifest(JMCLModpackManifest.INSTANCE).setGameVersion(game.getJar());
        return manifest;
    }

    private final static class HMCLModpack extends Modpack {
        @Override
        public Task<?> getInstallTask(DefaultDependencyManager dependencyManager, Path zipFile, String name, String iconUrl) {
            return new JMCLModpackInstallTask(((JMCLGameRepository) dependencyManager.getGameRepository()).getProfile(), zipFile, this, name);
        }
    }

}
