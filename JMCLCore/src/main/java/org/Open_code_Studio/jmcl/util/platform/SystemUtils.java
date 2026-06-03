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
package org.Open_code_Studio.jmcl.util.platform;

import org.Open_code_Studio.jmcl.task.Schedulers;
import org.Open_code_Studio.jmcl.util.Lang;
import org.Open_code_Studio.jmcl.util.function.ExceptionalFunction;
import org.Open_code_Studio.jmcl.util.io.IOUtils;
import org.jetbrains.annotations.Nullable;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.time.Duration;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import static org.Open_code_Studio.jmcl.util.logging.Logger.LOG;

public final class SystemUtils {
    private SystemUtils() {
    }

    public static @Nullable Path which(String command) {
        String path = System.getenv("PATH");
        if (path == null)
            return null;

        try {
            for (String item : path.split(File.pathSeparator)) {
                try {
                    Path program = Paths.get(item, command);
                    if (Files.isExecutable(program))
                        return program.toRealPath();
                } catch (Throwable ignored) {
                }
            }
        } catch (Throwable ignored) {
        }

        return null;
    }

    public static int callExternalProcess(String... command) throws IOException, InterruptedException {
        return callExternalProcess(Arrays.asList(command));
    }

    public static int callExternalProcess(List<String> command) throws IOException, InterruptedException {
        return callExternalProcess(new ProcessBuilder(command));
    }

    public static int callExternalProcess(ProcessBuilder processBuilder) throws IOException, InterruptedException {
        ManagedProcess managedProcess = new ManagedProcess(processBuilder);
        managedProcess.pumpInputStream(SystemUtils::onLogLine);
        managedProcess.pumpErrorStream(SystemUtils::onLogLine);
        return managedProcess.getProcess().waitFor();
    }

    private static final Duration DEFAULT_MAX_WAIT_TIME = Duration.ofSeconds(15);

    public static String run(String... command) throws Exception {
        return run(List.of(command), DEFAULT_MAX_WAIT_TIME);
    }

    public static String run(List<String> command, Duration maxWaitTime) throws Exception {
        return run(command, inputStream -> IOUtils.readFullyAsString(inputStream, OperatingSystem.NATIVE_CHARSET), maxWaitTime);
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert) throws Exception {
        return run(command, convert, DEFAULT_MAX_WAIT_TIME);
    }

    public static <T> T run(List<String> command, ExceptionalFunction<InputStream, T, ?> convert, Duration maxWaitTime) throws Exception {
        List<String> actualCommand = new ArrayList<>(command);
        
        if (OperatingSystem.CURRENT_OS == OperatingSystem.MACOS) {
            String executablePath = command.get(0);
            Path exePath = Paths.get(executablePath);
            
            if (!Files.isExecutable(exePath)) {
                try {
                    Set<PosixFilePermission> perms = Files.getPosixFilePermissions(exePath);
                    perms.add(PosixFilePermission.OWNER_EXECUTE);
                    perms.add(PosixFilePermission.GROUP_EXECUTE);
                    perms.add(PosixFilePermission.OTHERS_EXECUTE);
                    Files.setPosixFilePermissions(exePath, perms);
                    LOG.info("Fixed execute permission for: " + executablePath);
                } catch (Exception e) {
                    LOG.warning("Failed to set execute permission for: " + executablePath, e);
                }
            }

            List<String> wrappedCommand = new ArrayList<>();
            wrappedCommand.add("/bin/bash");
            wrappedCommand.add("-c");
            wrappedCommand.add(String.join(" ", command));
            actualCommand = wrappedCommand;
        } else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            List<String> wrappedCommand = new ArrayList<>();
            wrappedCommand.add("cmd.exe");
            wrappedCommand.add("/c");
            wrappedCommand.add(String.join(" ", command));
            actualCommand = wrappedCommand;
        }

        ProcessBuilder processBuilder = new ProcessBuilder(actualCommand)
                .redirectError(ProcessBuilder.Redirect.DISCARD);

        Process process = processBuilder.start();
        try {
            InputStream inputStream = process.getInputStream();
            CompletableFuture<T> future = CompletableFuture.supplyAsync(
                    Lang.wrap(() -> convert.apply(inputStream)),
                    Schedulers.io());

            if (!process.waitFor(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS))
                throw new TimeoutException();

            if (process.exitValue() != 0)
                throw new IOException("Bad exit code: " + process.exitValue());

            return future.get();
        } finally {
            if (process.isAlive())
                process.destroy();
        }
    }

    public static boolean supportJVMAttachment() {
        return Thread.currentThread().getContextClassLoader().getResource("com/sun/tools/attach/VirtualMachine.class") != null;
    }

    public static void onLogLine(String log) {
        LOG.info(log);
    }
}