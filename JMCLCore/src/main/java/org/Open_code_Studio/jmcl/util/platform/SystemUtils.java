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

            // Wrap with bash -c, quoting each argument individually to handle spaces in paths
            List<String> wrappedCommand = new ArrayList<>();
            wrappedCommand.add("/bin/bash");
            wrappedCommand.add("-c");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < command.size(); i++) {
                if (i > 0) sb.append(' ');
                String arg = command.get(i);
                // Quote arguments that contain spaces or special chars
                if (arg.contains(" ") || arg.contains("\t") || arg.contains("\\") || arg.contains("'")) {
                    sb.append('"');
                    // Escape double quotes and backticks
                    sb.append(arg.replace("\\", "\\\\").replace("\"", "\\\"").replace("`", "\\`").replace("$", "\\$"));
                    sb.append('"');
                } else {
                    sb.append(arg);
                }
            }
            wrappedCommand.add(sb.toString());
            actualCommand = wrappedCommand;
        } else if (OperatingSystem.CURRENT_OS == OperatingSystem.WINDOWS) {
            List<String> wrappedCommand = new ArrayList<>();
            wrappedCommand.add("cmd.exe");
            wrappedCommand.add("/c");
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < command.size(); i++) {
                if (i > 0) sb.append(' ');
                String arg = command.get(i);
                if (arg.contains(" ") || arg.contains("\t")) {
                    sb.append('"').append(arg.replace("\"", "\\\"")).append('"');
                } else {
                    sb.append(arg);
                }
            }
            wrappedCommand.add(sb.toString());
            actualCommand = wrappedCommand;
        }

        // Retry up to 3 times on macOS posix_spawn failures (intermittent kernel bug)
        int maxAttempts = OperatingSystem.CURRENT_OS == OperatingSystem.MACOS ? 3 : 1;
        IOException lastException = null;
        
        for (int attempt = 1; attempt <= maxAttempts; attempt++) {
            ProcessBuilder processBuilder = new ProcessBuilder(actualCommand)
                    .redirectErrorStream(true);

            try {
                Process process = processBuilder.start();
                try {
                    InputStream inputStream = process.getInputStream();
                    CompletableFuture<T> future = CompletableFuture.supplyAsync(
                            Lang.wrap(() -> convert.apply(inputStream)),
                            Schedulers.io());

                    if (!process.waitFor(maxWaitTime.toMillis(), TimeUnit.MILLISECONDS))
                        throw new TimeoutException();

                    // Read output even when exit code is non-zero, so we can include it in the error
                    T output;
                    String errorMsg = null;
                    try {
                        output = future.get();
                    } catch (Exception e) {
                        output = null;
                        errorMsg = "(failed to read output: " + e.getMessage() + ")";
                    }

                    if (process.exitValue() != 0)
                        throw new IOException("Bad exit code: " + process.exitValue()
                                + ". Process output: " + (errorMsg != null ? errorMsg : String.valueOf(output)));

                    return output;
                } finally {
                    if (process.isAlive())
                        process.destroy();
                }
            } catch (IOException e) {
                String msg = e.getMessage();
                if (msg != null && msg.contains("posix_spawn")) {
                    LOG.warning("posix_spawn failed (attempt " + attempt + "/" + maxAttempts + "), retrying...", e);
                    lastException = e;
                    try {
                        Thread.sleep(100L * attempt);
                    } catch (InterruptedException ignored) {
                        Thread.currentThread().interrupt();
                        throw e;
                    }
                } else {
                    throw e;
                }
            }
        }
        
        throw lastException;
    }

    public static boolean supportJVMAttachment() {
        return Thread.currentThread().getContextClassLoader().getResource("com/sun/tools/attach/VirtualMachine.class") != null;
    }

    public static void onLogLine(String log) {
        LOG.info(log);
    }
}