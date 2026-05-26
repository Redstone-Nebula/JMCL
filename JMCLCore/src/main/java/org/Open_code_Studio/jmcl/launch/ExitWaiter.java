/*
 * Hello Minecraft! Launcher
 * Copyright (C) 2020  huangyuhui <huanghongxun2008@126.com> and contributors
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
package org.Open_code_Studio.jmcl.launch;

import org.Open_code_Studio.jmcl.event.EventBus;
import org.Open_code_Studio.jmcl.event.JVMLaunchFailedEvent;
import org.Open_code_Studio.jmcl.event.ProcessExitedAbnormallyEvent;
import org.Open_code_Studio.jmcl.event.ProcessStoppedEvent;
import org.Open_code_Studio.jmcl.util.Log4jLevel;
import org.Open_code_Studio.jmcl.util.StringUtils;
import org.Open_code_Studio.jmcl.util.platform.ManagedProcess;
import org.Open_code_Studio.jmcl.util.platform.OperatingSystem;

import java.util.Collection;
import java.util.List;
import java.util.function.BiConsumer;

/**
 * @author huangyuhui
 */
final class ExitWaiter implements Runnable {

    private final ManagedProcess process;
    private final Collection<Thread> joins;
    private final BiConsumer<Integer, ProcessListener.ExitType> watcher;

    /**
     * Constructor.
     *
     * @param process the process to wait for
     * @param watcher the callback that will be called after process stops.
     */
    public ExitWaiter(ManagedProcess process, Collection<Thread> joins, BiConsumer<Integer, ProcessListener.ExitType> watcher) {
        this.process = process;
        this.joins = joins;
        this.watcher = watcher;
    }

    @Override
    public void run() {
        try {
            int exitCode = process.getProcess().waitFor();

            for (Thread thread : joins)
                thread.join();

            List<String> errorLines = process.getLines(Log4jLevel::guessLogLineError);
            ProcessListener.ExitType exitType;

            // LaunchWrapper will catch the exception logged and will exit normally.
            if (exitCode != 0 && StringUtils.containsOne(errorLines,
                    "Could not create the Java Virtual Machine.",
                    "Error occurred during initialization of VM",
                    "A fatal exception has occurred. Program will exit.")) {
                EventBus.EVENT_BUS.fireEvent(new JVMLaunchFailedEvent(this, process));
                exitType = ProcessListener.ExitType.JVM_ERROR;
            } else if (exitCode != 0 || StringUtils.containsOne(errorLines,
                    "Crash report saved to", "Could not save crash report to", "This crash report has been saved to:",
                    "Unable to launch", "An exception was thrown, the game will display an error screen and halt.")) {
                EventBus.EVENT_BUS.fireEvent(new ProcessExitedAbnormallyEvent(this, process));

                if (exitCode == 137 && OperatingSystem.CURRENT_OS.isLinuxOrBSD()) {
                    exitType = ProcessListener.ExitType.SIGKILL;
                } else {
                    exitType = ProcessListener.ExitType.APPLICATION_ERROR;
                }
            } else {
                exitType = ProcessListener.ExitType.NORMAL;
            }

            EventBus.EVENT_BUS.fireEvent(new ProcessStoppedEvent(this, process));

            watcher.accept(exitCode, exitType);
        } catch (InterruptedException e) {
            watcher.accept(1, ProcessListener.ExitType.INTERRUPTED);
        }
    }

}
