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
package org.Open_code_Studio.jmcl.ui.wizard;

import javafx.scene.Node;
import org.Open_code_Studio.jmcl.task.Task;
import org.Open_code_Studio.jmcl.util.SettingsMap;

public interface WizardDisplayer {
    default void onStart() {
    }

    default void onEnd() {
    }

    default void onCancel() {
    }

    void navigateTo(Node page, Navigation.NavigationDirection nav);

    void handleTask(SettingsMap settings, Task<?> task);
}
