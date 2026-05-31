/*
 * JMCL
 * Copyright (C) 2026 OCS
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
package org.Open_code_Studio.jmcl.ui.construct;

import javafx.event.ActionEvent;
import org.Open_code_Studio.jmcl.ui.FXUtils;

/// @author Glavo
public abstract class LineButtonBase extends LineComponent {
    private static final String DEFAULT_STYLE_CLASS = "line-button-base";

    protected final RipplerContainer ripplerContainer;

    public LineButtonBase() {
        this.getStyleClass().addAll(LineButtonBase.DEFAULT_STYLE_CLASS);

        this.ripplerContainer = new RipplerContainer(container);
        FXUtils.onClicked(this, this::fire);

        this.getChildren().setAll(ripplerContainer);
    }

    public void fire() {
        fireEvent(new ActionEvent());
    }
}
