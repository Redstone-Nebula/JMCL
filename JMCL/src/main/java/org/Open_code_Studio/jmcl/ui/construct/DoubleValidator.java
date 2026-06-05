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
package org.Open_code_Studio.jmcl.ui.construct;

import javafx.beans.NamedArg;
import org.Open_code_Studio.jmcl.util.Lang;
import org.Open_code_Studio.jmcl.util.StringUtils;

import java.util.function.Predicate;

public class DoubleValidator implements Predicate<String> {
    private final boolean nullable;

    public DoubleValidator() {
        this(false);
    }

    public DoubleValidator(@NamedArg("nullable") boolean nullable) {
        this.nullable = nullable;
    }

    @Override
    public boolean test(String text) {
        if (StringUtils.isBlank(text))
            return nullable;
        else
            return Lang.toDoubleOrNull(text) != null;
    }
}